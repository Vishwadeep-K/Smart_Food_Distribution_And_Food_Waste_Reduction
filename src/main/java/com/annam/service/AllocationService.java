package com.annam.service;

import com.annam.exception.DuplicateReservationException;
import com.annam.exception.InsufficientFoodException;
import com.annam.exception.InvalidAllocationException;
import com.annam.graph.Location;
import com.annam.graph.PathResult;
import com.annam.model.*;
import com.annam.policy.MatchResult;
import com.annam.repository.AllocationRepository;
import com.annam.repository.FoodBatchRepository;
import com.annam.repository.RecipientRepository;
import com.annam.repository.VehicleRepository;
import com.annam.util.IDGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
public class AllocationService {
    private final MatchingService matchingService;
    private final FoodBatchService foodBatchService;
    private final RecipientService recipientService;
    private final VehicleService vehicleService;
    private final RoadNetworkService roadNetworkService;
    private final SimulatedClockService clockService;
    private final AllocationRepository allocationRepository;
    private final FoodBatchRepository batchRepository;
    private final RecipientRepository recipientRepository;
    private final VehicleRepository vehicleRepository;

    public AllocationService(MatchingService matchingService, FoodBatchService foodBatchService,
                             RecipientService recipientService, VehicleService vehicleService,
                             RoadNetworkService roadNetworkService, SimulatedClockService clockService,
                             AllocationRepository allocationRepository, FoodBatchRepository batchRepository,
                             RecipientRepository recipientRepository, VehicleRepository vehicleRepository) {
        this.matchingService = matchingService;
        this.foodBatchService = foodBatchService;
        this.recipientService = recipientService;
        this.vehicleService = vehicleService;
        this.roadNetworkService = roadNetworkService;
        this.clockService = clockService;
        this.allocationRepository = allocationRepository;
        this.batchRepository = batchRepository;
        this.recipientRepository = recipientRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public List<FoodAllocation> getAllAllocations() {
        return allocationRepository.findAll();
    }

    public FoodAllocation getAllocationById(String id) {
        return allocationRepository.findById(id);
    }

    public List<AllocationRepository.HistoryRecordDto> getAllocationHistory() {
        return allocationRepository.findAllHistory();
    }

    /**
     * Atomically reserves food portions for an approved matching.
     * Transactional: If any step fails, all database changes rollback.
     */
    @Transactional
    public synchronized FoodAllocation reserveFood(String batchId, String recipientId, String vehicleId) {
        // 1. Evaluate domain and deadline feasibility
        MatchResult matchResult = matchingService.evaluateMatch(batchId, recipientId, vehicleId);
        if (!matchResult.isAccepted()) {
            throw new InvalidAllocationException("Cannot reserve: " + matchResult.getReason());
        }

        int portionsToReserve = matchResult.getAllocatedPortions();
        if (portionsToReserve <= 0) {
            throw new InsufficientFoodException("Allocated portion count is zero.");
        }

        FoodBatch batch = foodBatchService.getBatchById(batchId);
        Recipient recipient = recipientService.getRecipientById(recipientId);
        DeliveryVehicle vehicle = vehicleService.getVehicleById(vehicleId);

        // 2. Prevent over-allocation / duplicate reservation on food batch atomically in DB
        boolean reservedSuccessfully = batchRepository.reservePortionsConditionally(batchId, portionsToReserve);
        if (!reservedSuccessfully) {
            throw new DuplicateReservationException("Reservation failed: Batch " + batchId 
                    + " has insufficient portions or was already reserved by another request.");
        }

        // 3. Update recipient remaining demand in DB
        int newDemand = Math.max(0, recipient.getRemainingDemandPortions() - portionsToReserve);
        recipientRepository.updateDemand(recipientId, newDemand);

        // 4. Mark vehicle busy
        vehicleRepository.setAvailable(vehicleId, false);

        // 5. Create Allocation record in RESERVED status
        String allocationId = IDGenerator.nextAllocationId();
        PathResult route = matchResult.getRouteResult();
        LocalTime pickupTime = matchResult.getPickupTime();
        int handover = matchResult.getHandoverMinutes();

        FoodAllocation allocation = new FoodAllocation(
                allocationId, batch, recipient, vehicle, portionsToReserve, route,
                pickupTime, handover, AllocationStatus.RESERVED
        );
        allocationRepository.save(allocation);

        // 6. Record audit history
        allocationRepository.recordHistory(
                allocationId, "RESERVED",
                "Reserved " + portionsToReserve + " portions for " + recipient.getName() + " via " + vehicle.getName()
        );

        // 7. Synchronize in-memory models and PriorityQueue
        batch.reservePortions(portionsToReserve);
        recipient.fulfillDemand(portionsToReserve);
        vehicle.setAvailable(false);
        foodBatchService.syncQueueAfterPortionChange(batchId);

        return allocation;
    }

    /**
     * Confirms physical delivery of a reserved food allocation.
     */
    @Transactional
    public synchronized FoodAllocation confirmDelivery(String allocationId) {
        FoodAllocation allocation = allocationRepository.findById(allocationId);
        if (allocation == null) {
            throw new InvalidAllocationException("Allocation " + allocationId + " not found.");
        }
        if (allocation.getStatus() == AllocationStatus.DELIVERED) {
            throw new InvalidAllocationException("Allocation " + allocationId + " has already been DELIVERED.");
        }
        if (allocation.getStatus() != AllocationStatus.RESERVED) {
            throw new InvalidAllocationException("Allocation " + allocationId + " cannot be delivered from status " + allocation.getStatus());
        }

        int portions = allocation.getAllocatedPortions();
        String batchId = allocation.getFoodBatch().getBatchId();
        String vehicleId = allocation.getVehicle().getVehicleId();
        Location destLoc = allocation.getDestinationLocation();

        // 1. Commit batch delivered portions in DB
        batchRepository.confirmDelivered(batchId, portions);

        // 2. Update vehicle location and availability in DB
        vehicleRepository.updateLocationAndAvailability(vehicleId, destLoc.getLocationId(), true);

        // 3. Update allocation status to DELIVERED
        allocationRepository.updateStatus(allocationId, AllocationStatus.DELIVERED);

        // 4. Record audit history
        allocationRepository.recordHistory(
                allocationId, "DELIVERED",
                "Successfully delivered " + portions + " portions to " + allocation.getRecipient().getName()
        );

        // 5. Update domain model instances
        allocation.confirmDelivery();
        foodBatchService.syncQueueAfterPortionChange(batchId);

        return allocation;
    }
}
