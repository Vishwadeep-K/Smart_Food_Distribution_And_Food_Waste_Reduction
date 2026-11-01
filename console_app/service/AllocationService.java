package service;

import exception.DuplicateReservationException;
import exception.InvalidAllocationException;
import graph.RoadNetwork;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import model.AllocationStatus;
import model.DeliveryVehicle;
import model.FoodAllocation;
import model.FoodBatch;
import model.Recipient;
import policy.FoodMatchingPolicy;
import policy.MatchResult;
import util.IDGenerator;

/**
 * Service managing the lifecycle of allocations:
 * creation, feasibility verification, reservation, unallocated remainder tracking,
 * and delivery confirmation.
 */
public class AllocationService {
    private final FoodMatchingPolicy matchingPolicy;
    private final List<FoodAllocation> allocationHistory;
    private final List<FoodBatch> unallocatedRemainders;

    public AllocationService(FoodMatchingPolicy matchingPolicy) {
        if (matchingPolicy == null) {
            throw new IllegalArgumentException("FoodMatchingPolicy cannot be null.");
        }
        this.matchingPolicy = matchingPolicy;
        this.allocationHistory = new ArrayList<>();
        this.unallocatedRemainders = new ArrayList<>();
    }

    /**
     * Evaluates feasibility and creates a PROPOSED FoodAllocation if accepted.
     */
    public FoodAllocation createProposedAllocation(FoodBatch batch, Recipient recipient, DeliveryVehicle vehicle,
                                                   RoadNetwork roadNetwork, LocalTime currentTime, int handoverMins) {
        MatchResult result = matchingPolicy.evaluateMatch(batch, recipient, vehicle, roadNetwork, currentTime, handoverMins);
        if (!result.isAccepted()) {
            throw new InvalidAllocationException(result.getReason());
        }

        String allocationId = IDGenerator.nextAllocationId();
        FoodAllocation allocation = new FoodAllocation(
                allocationId,
                batch,
                recipient,
                vehicle,
                result.getAllocatedPortions(),
                result.getRouteResult(),
                result.getPickupTime(),
                result.getHandoverMinutes()
        );

        allocationHistory.add(allocation);
        return allocation;
    }

    /**
     * Reserves portions for a proposed allocation.
     * Prevents duplicate reservations and enforces state consistency.
     */
    public synchronized void reserveAllocation(FoodAllocation allocation) {
        if (allocation == null) {
            throw new IllegalArgumentException("Allocation cannot be null.");
        }
        if (allocation.getStatus() == AllocationStatus.RESERVED) {
            throw new DuplicateReservationException("Allocation " + allocation.getAllocationId() 
                    + " has already been reserved.");
        }
        if (allocation.getStatus() != AllocationStatus.PROPOSED) {
            throw new InvalidAllocationException("Cannot reserve allocation " + allocation.getAllocationId() 
                    + " in status: " + allocation.getStatus());
        }

        FoodBatch batch = allocation.getFoodBatch();
        Recipient recipient = allocation.getRecipient();
        DeliveryVehicle vehicle = allocation.getVehicle();
        int portions = allocation.getAllocatedPortions();

        // Check vehicle availability
        if (!vehicle.isAvailable()) {
            throw new InvalidAllocationException("Vehicle " + vehicle.getVehicleId() + " is no longer available.");
        }

        // Apply reservations atomically
        batch.reservePortions(portions);
        recipient.fulfillDemand(portions);
        vehicle.setAvailable(false);
        allocation.setStatus(AllocationStatus.RESERVED);

        // Retain unallocated remainder tracking
        if (batch.getAvailablePortions() > 0 && !unallocatedRemainders.contains(batch)) {
            unallocatedRemainders.add(batch);
        } else if (batch.getAvailablePortions() == 0) {
            unallocatedRemainders.remove(batch);
        }
    }

    /**
     * Confirms delivery of a reserved allocation.
     */
    public synchronized void confirmDelivery(FoodAllocation allocation) {
        if (allocation == null) {
            throw new IllegalArgumentException("Allocation cannot be null.");
        }
        allocation.confirmDelivery();
    }

    public List<FoodAllocation> getAllocationHistory() {
        return Collections.unmodifiableList(allocationHistory);
    }

    public List<FoodBatch> getUnallocatedRemainders() {
        // Return only batches that currently have available portions > 0 and are not expired
        List<FoodBatch> activeRemainders = new ArrayList<>();
        for (FoodBatch batch : unallocatedRemainders) {
            if (batch.getAvailablePortions() > 0 && !batch.isExpired()) {
                activeRemainders.add(batch);
            }
        }
        return Collections.unmodifiableList(activeRemainders);
    }
}
