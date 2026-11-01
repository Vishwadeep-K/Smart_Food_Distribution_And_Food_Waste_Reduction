package com.annam.controller;

import com.annam.model.FoodAllocation;
import com.annam.repository.AllocationRepository;
import com.annam.service.AllocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/allocations")
public class AllocationController {
    private final AllocationService allocationService;

    public AllocationController(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @GetMapping
    public ResponseEntity<List<AllocationDto>> getAllAllocations() {
        List<FoodAllocation> list = allocationService.getAllAllocations();
        return ResponseEntity.ok(list.stream().map(this::toDto).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAllocationById(@PathVariable String id) {
        FoodAllocation a = allocationService.getAllocationById(id);
        if (a == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDto(a));
    }

    @PostMapping("/reserve")
    public ResponseEntity<?> reserveFood(@RequestBody Map<String, String> body) {
        String batchId = body.get("batchId");
        String recipientId = body.get("recipientId");
        String vehicleId = body.get("vehicleId");

        if (batchId == null || recipientId == null || vehicleId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "batchId, recipientId, and vehicleId are required."));
        }

        FoodAllocation allocation = allocationService.reserveFood(batchId, recipientId, vehicleId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Food successfully reserved atomically.",
                "allocationId", allocation.getAllocationId(),
                "allocatedPortions", allocation.getAllocatedPortions(),
                "route", allocation.getRouteResult().getFormattedRoute(),
                "completionTime", allocation.getExpectedCompletionTime().toString().substring(0, 5),
                "remainingBatchPortions", allocation.getFoodBatch().getAvailablePortions(),
                "remainingRecipientDemand", allocation.getRecipient().getRemainingDemandPortions()
        ));
    }

    @PostMapping("/{id}/confirm-delivery")
    public ResponseEntity<?> confirmDelivery(@PathVariable String id) {
        FoodAllocation allocation = allocationService.confirmDelivery(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Physical delivery confirmed successfully for allocation " + id + ".",
                "status", allocation.getStatus().name()
        ));
    }

    @GetMapping("/history")
    public ResponseEntity<List<AllocationRepository.HistoryRecordDto>> getHistory() {
        return ResponseEntity.ok(allocationService.getAllocationHistory());
    }

    private AllocationDto toDto(FoodAllocation a) {
        return new AllocationDto(
                a.getAllocationId(),
                a.getFoodBatch().getBatchId(),
                a.getFoodBatch().getCategory().name(),
                a.getRecipient().getRecipientId(),
                a.getRecipient().getName(),
                a.getVehicle().getVehicleId(),
                a.getVehicle().getName(),
                a.getAllocatedPortions(),
                a.getPickupLocation().getName(),
                a.getDestinationLocation().getName(),
                a.getRouteResult() != null ? a.getRouteResult().getFormattedRoute() : "N/A",
                a.getTravelTimeMinutes(),
                a.getPickupTime().toString().substring(0, 5),
                a.getHandoverMinutes(),
                a.getExpectedCompletionTime().toString().substring(0, 5),
                a.getFoodBatch().getDeclaredUseByTime().toString().substring(0, 5),
                a.getStatus().name()
        );
    }

    public record AllocationDto(
            String allocationId,
            String batchId,
            String category,
            String recipientId,
            String recipientName,
            String vehicleId,
            String vehicleName,
            int allocatedPortions,
            String pickupLocation,
            String destinationLocation,
            String route,
            int travelTimeMinutes,
            String pickupTime,
            int handoverMinutes,
            String expectedCompletionTime,
            String useByDeadline,
            String status
    ) {}
}
