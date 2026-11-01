package com.annam.service;

import com.annam.graph.RoadNetwork;
import com.annam.model.DeliveryVehicle;
import com.annam.model.FoodBatch;
import com.annam.model.FoodCategory;
import com.annam.model.Recipient;
import com.annam.policy.FoodMatchingPolicy;
import com.annam.policy.MatchResult;
import com.annam.policy.TimeSensitiveFoodMatchingPolicy;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MatchingService {
    private final FoodBatchService foodBatchService;
    private final RecipientService recipientService;
    private final VehicleService vehicleService;
    private final RoadNetworkService roadNetworkService;
    private final SimulatedClockService clockService;
    private final FoodMatchingPolicy matchingPolicy;

    public MatchingService(FoodBatchService foodBatchService, RecipientService recipientService,
                           VehicleService vehicleService, RoadNetworkService roadNetworkService,
                           SimulatedClockService clockService) {
        this.foodBatchService = foodBatchService;
        this.recipientService = recipientService;
        this.vehicleService = vehicleService;
        this.roadNetworkService = roadNetworkService;
        this.clockService = clockService;
        this.matchingPolicy = new TimeSensitiveFoodMatchingPolicy();
    }

    public MatchResult evaluateMatch(String batchId, String recipientId, String vehicleId) {
        FoodBatch batch = foodBatchService.getBatchById(batchId);
        Recipient recipient = recipientService.getRecipientById(recipientId);
        DeliveryVehicle vehicle = vehicleService.getVehicleById(vehicleId);
        RoadNetwork network = roadNetworkService.getRoadNetwork();
        LocalTime now = clockService.getCurrentTime();
        int handover = clockService.getHandoverAllowanceMinutes();

        return matchingPolicy.evaluateMatch(batch, recipient, vehicle, network, now, handover);
    }

    public List<MatchEvaluationDto> findMatchesForRecipient(String recipientId, String categoryFilter, String preferredVehicleId) {
        Recipient recipient = recipientService.getRecipientById(recipientId);
        if (recipient == null) {
            return Collections.emptyList();
        }

        List<DeliveryVehicle> vehicles = vehicleService.getAllVehicles();
        DeliveryVehicle chosenVehicle = null;
        if (preferredVehicleId != null && !preferredVehicleId.trim().isEmpty()) {
            chosenVehicle = vehicleService.getVehicleById(preferredVehicleId);
        }
        if (chosenVehicle == null) {
            // Find first available vehicle with maximum capacity
            for (DeliveryVehicle v : vehicles) {
                if (v.isAvailable()) {
                    if (chosenVehicle == null || v.getPortionCapacity() > chosenVehicle.getPortionCapacity()) {
                        chosenVehicle = v;
                    }
                }
            }
        }
        if (chosenVehicle == null && !vehicles.isEmpty()) {
            chosenVehicle = vehicles.get(0); // for simulation display
        }

        // Process approved food batches strictly in Earliest-Use-By-First order (PriorityQueue order)
        List<FoodBatch> orderedBatches = foodBatchService.getApprovedPriorityQueueOrder();
        List<MatchEvaluationDto> evaluations = new ArrayList<>();
        RoadNetwork network = roadNetworkService.getRoadNetwork();
        LocalTime now = clockService.getCurrentTime();
        int handover = clockService.getHandoverAllowanceMinutes();

        for (FoodBatch batch : orderedBatches) {
            if (categoryFilter != null && !categoryFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(categoryFilter)) {
                try {
                    FoodCategory filterCat = FoodCategory.valueOf(categoryFilter.toUpperCase());
                    if (batch.getCategory() != filterCat) {
                        continue;
                    }
                } catch (Exception ignored) {}
            }

            MatchResult res = matchingPolicy.evaluateMatch(batch, recipient, chosenVehicle, network, now, handover);
            evaluations.add(new MatchEvaluationDto(
                    batch.getBatchId(),
                    batch.getCategory().name(),
                    batch.getCategory().getDisplayName(),
                    batch.getDonor().getName(),
                    batch.getPickupLocation().getName(),
                    recipient.getRecipientId(),
                    recipient.getName(),
                    recipient.getLocation().getName(),
                    chosenVehicle != null ? chosenVehicle.getVehicleId() : "NONE",
                    chosenVehicle != null ? chosenVehicle.getName() : "None",
                    chosenVehicle != null ? chosenVehicle.getPortionCapacity() : 0,
                    batch.getAvailablePortions(),
                    recipient.getRemainingDemandPortions(),
                    res.isAccepted() ? res.getAllocatedPortions() : 0,
                    res.isAccepted() ? res.getTravelTimeMinutes() : (res.getRouteResult() != null ? res.getRouteResult().getTotalTravelTimeMinutes() : 0),
                    res.isAccepted() && res.getRouteResult() != null ? res.getRouteResult().getFormattedRoute() : "N/A",
                    now.toString().substring(0, 5),
                    handover,
                    res.isAccepted() && res.getExpectedCompletionTime() != null ? res.getExpectedCompletionTime().toString().substring(0, 5) : "N/A",
                    batch.getDeclaredUseByTime().toString().substring(0, 5),
                    res.isAccepted() ? "ACCEPTED" : "REJECTED",
                    res.getReason()
            ));
        }

        return evaluations;
    }

    public record MatchEvaluationDto(
            String batchId,
            String category,
            String categoryDisplay,
            String donorName,
            String pickupLocation,
            String recipientId,
            String recipientName,
            String destinationLocation,
            String vehicleId,
            String vehicleName,
            int vehicleCapacity,
            int availablePortions,
            int remainingDemand,
            int allocatedPortions,
            int travelTimeMinutes,
            String route,
            String pickupTime,
            int handoverMinutes,
            String expectedCompletionTime,
            String useByDeadline,
            String status,
            String rejectionReason
    ) {}
}
