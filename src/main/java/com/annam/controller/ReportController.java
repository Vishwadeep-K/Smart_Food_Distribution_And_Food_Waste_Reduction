package com.annam.controller;

import com.annam.graph.RoadNetwork;
import com.annam.model.AllocationStatus;
import com.annam.model.FoodAllocation;
import com.annam.model.FoodBatch;
import com.annam.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final FoodBatchService foodBatchService;
    private final AllocationService allocationService;
    private final RoadNetworkService roadNetworkService;
    private final SimulatedClockService clockService;

    public ReportController(FoodBatchService foodBatchService, AllocationService allocationService,
                            RoadNetworkService roadNetworkService, SimulatedClockService clockService) {
        this.foodBatchService = foodBatchService;
        this.allocationService = allocationService;
        this.roadNetworkService = roadNetworkService;
        this.clockService = clockService;
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getReportsSummary() {
        List<FoodBatch> batches = foodBatchService.getAllBatches();
        List<FoodBatch> availableBatches = new ArrayList<>();
        List<FoodBatch> expiredBatches = new ArrayList<>();
        List<FoodBatch> unallocatedRemainders = new ArrayList<>();

        for (FoodBatch b : batches) {
            if (b.isExpired()) {
                expiredBatches.add(b);
            } else if (b.isApproved() && b.getAvailablePortions() > 0) {
                availableBatches.add(b);
                if (b.getReservedPortions() > 0 || b.getDeliveredPortions() > 0) {
                    unallocatedRemainders.add(b);
                }
            }
        }

        List<FoodAllocation> allocations = allocationService.getAllAllocations();
        List<FoodAllocation> pendingAllocations = new ArrayList<>();
        List<FoodAllocation> completedDeliveries = new ArrayList<>();

        for (FoodAllocation a : allocations) {
            if (a.getStatus() == AllocationStatus.RESERVED) {
                pendingAllocations.add(a);
            } else if (a.getStatus() == AllocationStatus.DELIVERED) {
                completedDeliveries.add(a);
            }
        }

        return ResponseEntity.ok(Map.of(
                "availableBatchesCount", availableBatches.size(),
                "expiredBatchesCount", expiredBatches.size(),
                "unallocatedRemaindersCount", unallocatedRemainders.size(),
                "pendingAllocationsCount", pendingAllocations.size(),
                "completedDeliveriesCount", completedDeliveries.size(),
                "totalBatches", batches.size(),
                "simulatedTime", clockService.getFormattedTime()
        ));
    }

    @GetMapping("/data-structures")
    public ResponseEntity<?> getDataStructuresInspection() {
        // 1. PriorityQueue demonstration
        List<FoodBatch> pqOrder = foodBatchService.getApprovedPriorityQueueOrder();
        List<Map<String, Object>> pqList = pqOrder.stream().map(b -> Map.<String, Object>of(
                "batchId", b.getBatchId(),
                "category", b.getCategory().name(),
                "useByTime", b.getDeclaredUseByTime().toString().substring(0, 5),
                "availablePortions", b.getAvailablePortions(),
                "pickupLocation", b.getPickupLocation().getName()
        )).toList();

        // 2. HashMap demonstration
        List<FoodBatch> all = foodBatchService.getAllBatches();
        List<String> registeredKeys = all.stream().map(FoodBatch::getBatchId).toList();

        // 3. Graph Adjacency List
        RoadNetwork net = roadNetworkService.getRoadNetwork();
        Map<String, List<String>> graphView = new LinkedHashMap<>();
        for (var entry : net.getAdjacencyList().entrySet()) {
            List<String> edgeStrings = entry.getValue().stream()
                    .map(e -> e.getDestination().getName() + " (" + e.getTravelTimeMinutes() + "m)")
                    .toList();
            graphView.put(net.getLocation(entry.getKey()).getName(), edgeStrings);
        }

        return ResponseEntity.ok(Map.of(
                "priorityQueueOrder", pqList,
                "priorityQueueExplanation", "Java PriorityQueue orders approved batches by earliest use-by time first. If times are equal, deterministic tie-breaking uses Batch ID. Whenever ranking fields change, the element is removed and reinserted.",
                "hashMapLookupKeys", registeredKeys,
                "hashMapExplanation", "Java HashMap provides O(1) average-time lookups for FoodBatches, Recipients, Donors, and Vehicles using unique primary keys.",
                "weightedGraphAdjacencyList", graphView,
                "graphExplanation", "Custom Adjacency-List weighted graph where nodes represent geographic road points and edge weights represent strictly positive travel times in minutes.",
                "dijkstraExplanation", "Classical greedy shortest-path algorithm using a Min-Heap (PriorityQueue) to determine minimum travel times and verify deadline feasibility before making reservations."
        ));
    }
}
