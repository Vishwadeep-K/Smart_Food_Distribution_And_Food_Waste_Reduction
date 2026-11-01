package com.annam.controller;

import com.annam.graph.Location;
import com.annam.model.ApprovalStatus;
import com.annam.model.Donor;
import com.annam.model.FoodBatch;
import com.annam.model.FoodCategory;
import com.annam.service.DonorService;
import com.annam.service.FoodBatchService;
import com.annam.service.RoadNetworkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batches")
public class FoodBatchController {
    private final FoodBatchService foodBatchService;
    private final DonorService donorService;
    private final RoadNetworkService roadNetworkService;

    public FoodBatchController(FoodBatchService foodBatchService, DonorService donorService,
                               RoadNetworkService roadNetworkService) {
        this.foodBatchService = foodBatchService;
        this.donorService = donorService;
        this.roadNetworkService = roadNetworkService;
    }

    @GetMapping
    public ResponseEntity<List<FoodBatchDto>> getAllBatches(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String donorId,
            @RequestParam(required = false) String approval,
            @RequestParam(required = false) Boolean expired
    ) {
        List<FoodBatch> list = foodBatchService.getAllBatches();
        List<FoodBatchDto> dtos = list.stream()
                .filter(b -> category == null || category.equalsIgnoreCase("ALL") || b.getCategory().name().equalsIgnoreCase(category))
                .filter(b -> donorId == null || donorId.equalsIgnoreCase("ALL") || b.getDonor().getDonorId().equalsIgnoreCase(donorId))
                .filter(b -> approval == null || approval.equalsIgnoreCase("ALL") || b.getApprovalStatus().name().equalsIgnoreCase(approval))
                .filter(b -> expired == null || b.isExpired() == expired)
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/queue")
    public ResponseEntity<List<FoodBatchDto>> getPriorityQueueOrder() {
        List<FoodBatch> list = foodBatchService.getApprovedPriorityQueueOrder();
        return ResponseEntity.ok(list.stream().map(this::toDto).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBatchById(@PathVariable String id) {
        FoodBatch b = foodBatchService.getBatchById(id);
        if (b == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDto(b));
    }

    @PostMapping
    public ResponseEntity<?> createBatch(@RequestBody Map<String, Object> body) {
        String id = (String) body.get("batchId");
        String donorId = (String) body.get("donorId");
        String categoryStr = (String) body.get("category");
        Number portionsNum = (Number) body.get("totalPortions");
        String useByStr = (String) body.get("useByTime");
        String locId = (String) body.get("pickupLocationId");
        String approvalStr = (String) body.getOrDefault("approvalStatus", "NOT_APPROVED");

        if (id == null || donorId == null || categoryStr == null || portionsNum == null || useByStr == null || locId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "All batch fields are required."));
        }

        Donor donor = donorService.getDonorById(donorId);
        if (donor == null) return ResponseEntity.badRequest().body(Map.of("message", "Donor not found: " + donorId));

        Location loc = roadNetworkService.getLocation(locId);
        if (loc == null) return ResponseEntity.badRequest().body(Map.of("message", "Location not found: " + locId));

        FoodCategory category;
        try {
            category = FoodCategory.valueOf(categoryStr.toUpperCase());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid category: " + categoryStr));
        }

        LocalTime useBy;
        try {
            if (useByStr.length() == 5) useByStr += ":00";
            useBy = LocalTime.parse(useByStr);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid time format (use HH:mm): " + useByStr));
        }

        ApprovalStatus status = ApprovalStatus.NOT_APPROVED;
        try {
            status = ApprovalStatus.valueOf(approvalStr.toUpperCase());
        } catch (Exception ignored) {}

        String foodName = (String) body.getOrDefault("foodName", category.getDisplayName());
        String notes = (String) body.get("notes");
        String prepTimeStr = (String) body.get("preparationTime");
        LocalTime prepTime = null;
        if (prepTimeStr != null && !prepTimeStr.trim().isEmpty()) {
            if (prepTimeStr.length() == 5) prepTimeStr += ":00";
            try {
                prepTime = LocalTime.parse(prepTimeStr);
            } catch (Exception ignored) {}
        }

        FoodBatch batch = new FoodBatch(id, category, foodName, portionsNum.intValue(), portionsNum.intValue(),
                0, 0, prepTime, useBy, donor, loc, notes, status, false, java.time.LocalDateTime.now());
        foodBatchService.saveBatch(batch);
        return ResponseEntity.ok(Map.of("success", true, "message", "Batch created successfully.", "batchId", id.toUpperCase()));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<FoodBatchDto>> getPendingBatches() {
        List<FoodBatch> list = foodBatchService.getAllBatches();
        List<FoodBatchDto> pending = list.stream()
                .filter(b -> b.getApprovalStatus() == ApprovalStatus.NOT_APPROVED && !b.isExpired())
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(pending);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveBatch(@PathVariable String id) {
        foodBatchService.approveBatch(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Batch " + id + " has been APPROVED for distribution."));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectBatch(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) String reason
    ) {
        String finalReason = null;
        if (body != null && body.containsKey("reason")) {
            finalReason = body.get("reason");
        } else if (reason != null) {
            finalReason = reason;
        }

        if (finalReason == null || finalReason.trim().isEmpty()) {
            finalReason = "Rejected by Administrator: Requirements not met.";
        }

        foodBatchService.rejectBatch(id, finalReason);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Batch " + id + " has been REJECTED. Reason: " + finalReason,
                "reason", finalReason
        ));
    }

    private FoodBatchDto toDto(FoodBatch b) {
        return new FoodBatchDto(
                b.getBatchId(),
                b.getFoodName(),
                b.getDonor().getDonorId(),
                b.getDonor().getName(),
                b.getCategory().name(),
                b.getCategory().getDisplayName(),
                b.getTotalPortions(),
                b.getAvailablePortions(),
                b.getReservedPortions(),
                b.getDeliveredPortions(),
                b.getPreparationTime() != null ? b.getPreparationTime().toString().substring(0, 5) : "--",
                b.getDeclaredUseByTime().toString().substring(0, 5),
                b.getPickupLocation().getLocationId(),
                b.getPickupLocation().getName(),
                b.getNotes(),
                b.getRejectionReason(),
                b.getApprovalStatus().name(),
                b.isExpired()
        );
    }

    public record FoodBatchDto(
            String batchId,
            String foodName,
            String donorId,
            String donorName,
            String category,
            String categoryDisplayName,
            int totalPortions,
            int availablePortions,
            int reservedPortions,
            int deliveredPortions,
            String preparationTime,
            String declaredUseByTime,
            String pickupLocationId,
            String pickupLocationName,
            String notes,
            String rejectionReason,
            String approvalStatus,
            boolean expired
    ) {}
}
