package model;

import exception.FoodExpiredException;
import exception.InsufficientFoodException;
import exception.InvalidAllocationException;
import graph.Location;
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
    private final int totalPortions;
    private int availablePortions;
    private int reservedPortions;
    private int deliveredPortions;
    private final LocalTime declaredUseByTime;
    private final Donor donor;
    private final Location pickupLocation;
    private ApprovalStatus approvalStatus;
    private boolean expired;

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
    }

    public String getBatchId() {
        return batchId;
    }

    public FoodCategory getCategory() {
        return category;
    }

    public int getTotalPortions() {
        return totalPortions;
    }

    public synchronized int getAvailablePortions() {
        return availablePortions;
    }

    public synchronized int getReservedPortions() {
        return reservedPortions;
    }

    public synchronized int getDeliveredPortions() {
        return deliveredPortions;
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

    /**
     * Marks the batch as expired.
     * Makes the batch permanently unavailable for distribution or reservation.
     */
    public synchronized void markExpired() {
        this.expired = true;
        this.availablePortions = 0;
    }

    /**
     * Encapsulated reservation logic.
     * Decreases available portions and increases reserved portions.
     * Strictly enforces business invariants:
     * - Cannot reserve from unapproved batch
     * - Cannot reserve from expired batch
     * - Cannot reserve non-positive portions
     * - Cannot reserve more portions than available
     */
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

    /**
     * Releases previously reserved portions back to available portions.
     */
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

    /**
     * Confirms delivery of reserved portions.
     */
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

    /**
     * Deterministic natural ordering for PriorityQueue:
     * 1. Earlier declared use-by time has higher priority (orders first).
     * 2. Deterministic tie-breaker: Batch ID lexicographical ascending order.
     */
    @Override
    public int compareTo(FoodBatch other) {
        if (other == null) return -1;
        int timeCmp = this.declaredUseByTime.compareTo(other.declaredUseByTime);
        if (timeCmp != 0) {
            return timeCmp;
        }
        // Deterministic tie-breaker
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
