package com.annam.model;

import com.annam.exception.FoodExpiredException;
import com.annam.exception.InsufficientFoodException;
import com.annam.exception.InvalidAllocationException;
import com.annam.graph.Location;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Represents an individual batch of surplus food.
 * Strictly encapsulates portion states, expiry, and approval.
 * Implements Comparable for deterministic PriorityQueue ordering.
 */
public class FoodBatch implements Comparable<FoodBatch> {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final String batchId;
    private final FoodCategory category;
    private String foodName;
    private final int totalPortions;
    private int availablePortions;
    private int reservedPortions;
    private int deliveredPortions;
    private LocalTime preparationTime;
    private final LocalTime declaredUseByTime;
    private final Donor donor;
    private final Location pickupLocation;
    private String notes;
    private String rejectionReason;
    private ApprovalStatus approvalStatus;
    private boolean expired;
    private LocalDateTime createdAt;

    public FoodBatch(String batchId, FoodCategory category, int totalPortions, LocalTime declaredUseByTime,
                     Donor donor, Location pickupLocation, ApprovalStatus approvalStatus) {
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch ID cannot be null or empty.");
        }
        if (category == null) {
            throw new IllegalArgumentException("Food category cannot be null.");
        }
        if (totalPortions <= 0) {
            throw new IllegalArgumentException("Total portions must be greater than zero.");
        }
        if (declaredUseByTime == null) {
            throw new IllegalArgumentException("Declared use-by time cannot be null.");
        }
        if (donor == null) {
            throw new IllegalArgumentException("Donor cannot be null.");
        }
        if (pickupLocation == null) {
            throw new IllegalArgumentException("Pickup location cannot be null.");
        }

        this.batchId = batchId.trim().toUpperCase();
        this.category = category;
        this.totalPortions = totalPortions;
        this.availablePortions = totalPortions;
        this.reservedPortions = 0;
        this.deliveredPortions = 0;
        this.declaredUseByTime = declaredUseByTime;
        this.donor = donor;
        this.pickupLocation = pickupLocation;
        this.approvalStatus = approvalStatus != null ? approvalStatus : ApprovalStatus.NOT_APPROVED;
        this.expired = false;
        this.createdAt = LocalDateTime.now();
    }

    public FoodBatch(String batchId, FoodCategory category, int totalPortions, int availablePortions,
                     int reservedPortions, int deliveredPortions, LocalTime declaredUseByTime,
                     Donor donor, Location pickupLocation, ApprovalStatus approvalStatus, boolean expired) {
        this(batchId, category, null, totalPortions, availablePortions, reservedPortions, deliveredPortions,
                null, declaredUseByTime, donor, pickupLocation, null, null, approvalStatus, expired, LocalDateTime.now());
    }

    public FoodBatch(String batchId, FoodCategory category, String foodName, int totalPortions, int availablePortions,
                     int reservedPortions, int deliveredPortions, LocalTime preparationTime, LocalTime declaredUseByTime,
                     Donor donor, Location pickupLocation, String notes, ApprovalStatus approvalStatus, boolean expired,
                     LocalDateTime createdAt) {
        this(batchId, category, foodName, totalPortions, availablePortions, reservedPortions, deliveredPortions,
                preparationTime, declaredUseByTime, donor, pickupLocation, notes, null, approvalStatus, expired, createdAt);
    }

    public FoodBatch(String batchId, FoodCategory category, String foodName, int totalPortions, int availablePortions,
                     int reservedPortions, int deliveredPortions, LocalTime preparationTime, LocalTime declaredUseByTime,
                     Donor donor, Location pickupLocation, String notes, String rejectionReason, ApprovalStatus approvalStatus,
                     boolean expired, LocalDateTime createdAt) {
        this.batchId = batchId.trim().toUpperCase();
        this.category = category;
        this.foodName = foodName != null ? foodName.trim() : (category != null ? category.getDisplayName() : "Surplus Food");
        this.totalPortions = totalPortions;
        this.availablePortions = availablePortions;
        this.reservedPortions = reservedPortions;
        this.deliveredPortions = deliveredPortions;
        this.preparationTime = preparationTime;
        this.declaredUseByTime = declaredUseByTime;
        this.donor = donor;
        this.pickupLocation = pickupLocation;
        this.notes = notes;
        this.rejectionReason = rejectionReason;
        this.approvalStatus = approvalStatus != null ? approvalStatus : ApprovalStatus.NOT_APPROVED;
        this.expired = expired;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public String getBatchId() {
        return batchId;
    }

    public FoodCategory getCategory() {
        return category;
    }

    public String getFoodName() {
        return foodName != null ? foodName : (category != null ? category.getDisplayName() : "Surplus Food");
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public LocalTime getPreparationTime() {
        return preparationTime;
    }

    public void setPreparationTime(LocalTime preparationTime) {
        this.preparationTime = preparationTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getTotalPortions() {
        return totalPortions;
    }

    public synchronized int getAvailablePortions() {
        return availablePortions;
    }

    public synchronized void setAvailablePortions(int availablePortions) {
        this.availablePortions = Math.max(0, availablePortions);
    }

    public synchronized int getReservedPortions() {
        return reservedPortions;
    }

    public synchronized void setReservedPortions(int reservedPortions) {
        this.reservedPortions = Math.max(0, reservedPortions);
    }

    public synchronized int getDeliveredPortions() {
        return deliveredPortions;
    }

    public synchronized void setDeliveredPortions(int deliveredPortions) {
        this.deliveredPortions = Math.max(0, deliveredPortions);
    }

    public LocalTime getDeclaredUseByTime() {
        return declaredUseByTime;
    }

    public Donor getDonor() {
        return donor;
    }

    public Location getPickupLocation() {
        return pickupLocation;
    }

    public synchronized ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public synchronized boolean isApproved() {
        return approvalStatus == ApprovalStatus.APPROVED;
    }

    public synchronized void setApprovalStatus(ApprovalStatus approvalStatus) {
        if (this.expired && approvalStatus == ApprovalStatus.APPROVED) {
            throw new InvalidAllocationException("Cannot approve batch " + batchId + " because it is already EXPIRED.");
        }
        this.approvalStatus = approvalStatus;
    }

    public synchronized boolean isExpired() {
        return expired;
    }

    public synchronized void markExpired() {
        this.expired = true;
        this.availablePortions = 0;
    }

    public synchronized void reservePortions(int portions) {
        if (portions <= 0) {
            throw new IllegalArgumentException("Portions to reserve must be positive (> 0).");
        }
        if (this.expired) {
            throw new FoodExpiredException("Cannot reserve from batch " + batchId + ": batch has expired.");
        }
        if (this.approvalStatus != ApprovalStatus.APPROVED) {
            throw new InvalidAllocationException("Cannot reserve from batch " + batchId + ": batch is NOT APPROVED.");
        }
        if (portions > this.availablePortions) {
            throw new InsufficientFoodException("Cannot reserve " + portions + " portions from batch " + batchId 
                    + ". Only " + this.availablePortions + " portions available.");
        }

        this.availablePortions -= portions;
        this.reservedPortions += portions;
    }

    public synchronized void releaseReservation(int portions) {
        if (portions <= 0) return;
        if (portions > this.reservedPortions) {
            throw new IllegalArgumentException("Cannot release more portions than currently reserved.");
        }
        this.reservedPortions -= portions;
        if (!this.expired) {
            this.availablePortions += portions;
        }
    }

    public synchronized void confirmDelivered(int portions) {
        if (portions <= 0) {
            throw new IllegalArgumentException("Portions delivered must be positive.");
        }
        if (portions > this.reservedPortions) {
            throw new InvalidAllocationException("Cannot deliver " + portions + " portions; only " 
                    + this.reservedPortions + " portions currently reserved.");
        }
        this.reservedPortions -= portions;
        this.deliveredPortions += portions;
    }

    @Override
    public int compareTo(FoodBatch other) {
        if (other == null) return -1;
        int timeCmp = this.declaredUseByTime.compareTo(other.declaredUseByTime);
        if (timeCmp != 0) {
            return timeCmp;
        }
        return this.batchId.compareTo(other.batchId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FoodBatch other)) return false;
        return batchId.equalsIgnoreCase(other.batchId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(batchId.toUpperCase());
    }

    @Override
    public String toString() {
        return String.format("%s | %-12s | Avail: %2d/%2d | Use-By: %s | Appr: %-12s | Exp: %-5s",
                batchId, category.name(), availablePortions, totalPortions, 
                declaredUseByTime.format(TIME_FMT), approvalStatus, expired);
    }
}
