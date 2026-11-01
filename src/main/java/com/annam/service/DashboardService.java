package com.annam.service;

import com.annam.model.AllocationStatus;
import com.annam.model.FoodAllocation;
import com.annam.model.FoodBatch;
import com.annam.model.FoodCategory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DashboardService {
    private final DonorService donorService;
    private final RecipientService recipientService;
    private final VehicleService vehicleService;
    private final FoodBatchService foodBatchService;
    private final AllocationService allocationService;
    private final SimulatedClockService clockService;

    public DashboardService(DonorService donorService, RecipientService recipientService,
                            VehicleService vehicleService, FoodBatchService foodBatchService,
                            AllocationService allocationService, SimulatedClockService clockService) {
        this.donorService = donorService;
        this.recipientService = recipientService;
        this.vehicleService = vehicleService;
        this.foodBatchService = foodBatchService;
        this.allocationService = allocationService;
        this.clockService = clockService;
    }

    public DashboardStatsDto getDashboardStats() {
        int totalDonors = donorService.getAllDonors().size();
        int totalRecipients = recipientService.getAllRecipients().size();
        int totalVehicles = vehicleService.getAllVehicles().size();

        List<FoodBatch> batches = foodBatchService.getAllBatches();
        int totalBatches = batches.size();
        int totalPortionsDonated = 0;
        int availablePortions = 0;
        int reservedPortions = 0;
        int deliveredPortions = 0;
        int expiredBatches = 0;
        int pendingApprovals = 0;
        int expiringBatches = 0;

        java.time.LocalTime currentClock = clockService.getCurrentTime();

        Map<String, Integer> portionsByCategory = new LinkedHashMap<>();
        for (FoodCategory cat : FoodCategory.values()) {
            portionsByCategory.put(cat.name(), 0);
        }

        for (FoodBatch b : batches) {
            totalPortionsDonated += b.getTotalPortions();
            availablePortions += b.getAvailablePortions();
            reservedPortions += b.getReservedPortions();
            deliveredPortions += b.getDeliveredPortions();

            if (b.isExpired()) {
                expiredBatches++;
            } else if (b.getApprovalStatus() == com.annam.model.ApprovalStatus.NOT_APPROVED) {
                pendingApprovals++;
            } else if (b.getApprovalStatus() == com.annam.model.ApprovalStatus.APPROVED && b.getAvailablePortions() > 0) {
                // Check if expiring within 60 minutes
                long diffMinutes = java.time.Duration.between(currentClock, b.getDeclaredUseByTime()).toMinutes();
                if (diffMinutes > 0 && diffMinutes <= 60) {
                    expiringBatches++;
                }
            }

            portionsByCategory.put(b.getCategory().name(),
                    portionsByCategory.getOrDefault(b.getCategory().name(), 0) + b.getTotalPortions());
        }

        List<FoodAllocation> allocations = allocationService.getAllAllocations();
        int pendingAllocations = 0;
        int completedAllocations = 0;
        for (FoodAllocation a : allocations) {
            if (a.getStatus() == AllocationStatus.RESERVED) {
                pendingAllocations++;
            } else if (a.getStatus() == AllocationStatus.DELIVERED) {
                completedAllocations++;
            }
        }

        int foodWasteAvoidedPortions = deliveredPortions + reservedPortions;

        return new DashboardStatsDto(
                totalDonors,
                totalRecipients,
                totalVehicles,
                totalBatches,
                totalPortionsDonated,
                availablePortions,
                reservedPortions,
                deliveredPortions,
                pendingApprovals,
                expiringBatches,
                expiredBatches,
                pendingAllocations,
                completedAllocations,
                foodWasteAvoidedPortions,
                clockService.getFormattedTime(),
                clockService.getHandoverAllowanceMinutes(),
                portionsByCategory
        );
    }

    public record DashboardStatsDto(
            int totalDonors,
            int totalRecipients,
            int totalVehicles,
            int totalBatches,
            int totalPortionsDonated,
            int availablePortions,
            int reservedPortions,
            int deliveredPortions,
            int pendingApprovals,
            int expiringBatches,
            int expiredBatches,
            int pendingAllocations,
            int completedAllocations,
            int foodWasteAvoidedPortions,
            String simulatedTime,
            int handoverAllowanceMinutes,
            Map<String, Integer> categoryPortions
    ) {}
}
