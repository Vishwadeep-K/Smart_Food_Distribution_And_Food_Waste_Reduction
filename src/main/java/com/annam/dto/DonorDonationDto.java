package com.annam.dto;

import java.util.ArrayList;
import java.util.List;

public class DonorDonationDto {
    private String batchId;
    private String foodName;
    private String category;
    private String categoryDisplayName;
    private int totalPortions;
    private int availablePortions;
    private int reservedPortions;
    private int deliveredPortions;
    private int remainingPortions;
    private String preparationTime;
    private String declaredUseByTime;
    private String pickupLocationId;
    private String pickupLocationName;
    private String notes;
    private String rejectionReason;
    private String approvalStatus;
    private String displayApprovalStatus;
    private String deliveryStatus;
    private boolean expired;
    private String createdAt;
    private List<AllocationDetailDto> allocations = new ArrayList<>();

    public DonorDonationDto() {}

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryDisplayName() {
        return categoryDisplayName;
    }

    public void setCategoryDisplayName(String categoryDisplayName) {
        this.categoryDisplayName = categoryDisplayName;
    }

    public int getTotalPortions() {
        return totalPortions;
    }

    public void setTotalPortions(int totalPortions) {
        this.totalPortions = totalPortions;
    }

    public int getAvailablePortions() {
        return availablePortions;
    }

    public void setAvailablePortions(int availablePortions) {
        this.availablePortions = availablePortions;
    }

    public int getReservedPortions() {
        return reservedPortions;
    }

    public void setReservedPortions(int reservedPortions) {
        this.reservedPortions = reservedPortions;
    }

    public int getDeliveredPortions() {
        return deliveredPortions;
    }

    public void setDeliveredPortions(int deliveredPortions) {
        this.deliveredPortions = deliveredPortions;
    }

    public String getPreparationTime() {
        return preparationTime;
    }

    public void setPreparationTime(String preparationTime) {
        this.preparationTime = preparationTime;
    }

    public String getDeclaredUseByTime() {
        return declaredUseByTime;
    }

    public void setDeclaredUseByTime(String declaredUseByTime) {
        this.declaredUseByTime = declaredUseByTime;
    }

    public String getPickupLocationId() {
        return pickupLocationId;
    }

    public void setPickupLocationId(String pickupLocationId) {
        this.pickupLocationId = pickupLocationId;
    }

    public String getPickupLocationName() {
        return pickupLocationName;
    }

    public void setPickupLocationName(String pickupLocationName) {
        this.pickupLocationName = pickupLocationName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getDisplayApprovalStatus() {
        return displayApprovalStatus;
    }

    public void setDisplayApprovalStatus(String displayApprovalStatus) {
        this.displayApprovalStatus = displayApprovalStatus;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public int getRemainingPortions() {
        return remainingPortions;
    }

    public void setRemainingPortions(int remainingPortions) {
        this.remainingPortions = remainingPortions;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public List<AllocationDetailDto> getAllocations() {
        return allocations;
    }

    public void setAllocations(List<AllocationDetailDto> allocations) {
        this.allocations = allocations != null ? allocations : new ArrayList<>();
    }

    public static class AllocationDetailDto {
        private String allocationId;
        private String recipientName;
        private String recipientType;
        private String vehicleName;
        private int allocatedPortions;
        private String pickupLocationName;
        private String destinationLocationName;
        private String pickupTime;
        private String expectedCompletionTime;
        private String status;

        public AllocationDetailDto() {}

        public AllocationDetailDto(String allocationId, String recipientName, String recipientType,
                                   String vehicleName, int allocatedPortions, String pickupLocationName,
                                   String destinationLocationName, String pickupTime,
                                   String expectedCompletionTime, String status) {
            this.allocationId = allocationId;
            this.recipientName = recipientName;
            this.recipientType = recipientType;
            this.vehicleName = vehicleName;
            this.allocatedPortions = allocatedPortions;
            this.pickupLocationName = pickupLocationName;
            this.destinationLocationName = destinationLocationName;
            this.pickupTime = pickupTime;
            this.expectedCompletionTime = expectedCompletionTime;
            this.status = status;
        }

        public String getAllocationId() { return allocationId; }
        public void setAllocationId(String allocationId) { this.allocationId = allocationId; }
        public String getRecipientName() { return recipientName; }
        public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
        public String getRecipientType() { return recipientType; }
        public void setRecipientType(String recipientType) { this.recipientType = recipientType; }
        public String getVehicleName() { return vehicleName; }
        public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }
        public int getAllocatedPortions() { return allocatedPortions; }
        public void setAllocatedPortions(int allocatedPortions) { this.allocatedPortions = allocatedPortions; }
        public String getPickupLocationName() { return pickupLocationName; }
        public void setPickupLocationName(String pickupLocationName) { this.pickupLocationName = pickupLocationName; }
        public String getDestinationLocationName() { return destinationLocationName; }
        public void setDestinationLocationName(String destinationLocationName) { this.destinationLocationName = destinationLocationName; }
        public String getPickupTime() { return pickupTime; }
        public void setPickupTime(String pickupTime) { this.pickupTime = pickupTime; }
        public String getExpectedCompletionTime() { return expectedCompletionTime; }
        public void setExpectedCompletionTime(String expectedCompletionTime) { this.expectedCompletionTime = expectedCompletionTime; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
