package model;

import exception.InvalidAllocationException;
import graph.Location;
import graph.PathResult;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents the assignment of a specific portion quantity from a FoodBatch
 * to a Recipient, delivered via a DeliveryVehicle following a Dijkstra-calculated route.
 * Demonstrates Object Composition and state lifecycle management.
 */
public class FoodAllocation {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final String allocationId;
    private final FoodBatch foodBatch;
    private final Recipient recipient;
    private final DeliveryVehicle vehicle;
    private final int allocatedPortions;
    private final Location pickupLocation;
    private final Location destinationLocation;
    private final PathResult routeResult;
    private final int travelTimeMinutes;
    private final LocalTime pickupTime;
    private final int handoverMinutes;
    private final LocalTime expectedCompletionTime;
    private AllocationStatus status;

    public FoodAllocation(String allocationId, FoodBatch foodBatch, Recipient recipient,
                          DeliveryVehicle vehicle, int allocatedPortions, PathResult routeResult,
                          LocalTime pickupTime, int handoverMinutes) {
        if (allocationId == null || allocationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Allocation ID cannot be null or empty.");
        }
        if (foodBatch == null) {
            throw new IllegalArgumentException("FoodBatch cannot be null.");
        }
        if (recipient == null) {
            throw new IllegalArgumentException("Recipient cannot be null.");
        }
        if (vehicle == null) {
            throw new IllegalArgumentException("DeliveryVehicle cannot be null.");
        }
        if (allocatedPortions <= 0) {
            throw new IllegalArgumentException("Allocated portions must be greater than zero.");
        }
        if (routeResult == null || !routeResult.isReachable()) {
            throw new IllegalArgumentException("Valid reachable route is required for allocation.");
        }
        if (pickupTime == null) {
            throw new IllegalArgumentException("Pickup time cannot be null.");
        }
        if (handoverMinutes < 0) {
            throw new IllegalArgumentException("Handover time cannot be negative.");
        }

        this.allocationId = allocationId.trim().toUpperCase();
        this.foodBatch = foodBatch;
        this.recipient = recipient;
        this.vehicle = vehicle;
        this.allocatedPortions = allocatedPortions;
        this.pickupLocation = foodBatch.getPickupLocation();
        this.destinationLocation = recipient.getLocation();
        this.routeResult = routeResult;
        this.travelTimeMinutes = routeResult.getTotalTravelTimeMinutes();
        this.pickupTime = pickupTime;
        this.handoverMinutes = handoverMinutes;
        this.expectedCompletionTime = pickupTime.plusMinutes((long) travelTimeMinutes + handoverMinutes);
        this.status = AllocationStatus.PROPOSED;
    }

    public String getAllocationId() {
        return allocationId;
    }

    public FoodBatch getFoodBatch() {
        return foodBatch;
    }

    public Recipient getRecipient() {
        return recipient;
    }

    public DeliveryVehicle getVehicle() {
        return vehicle;
    }

    public int getAllocatedPortions() {
        return allocatedPortions;
    }

    public Location getPickupLocation() {
        return pickupLocation;
    }

    public Location getDestinationLocation() {
        return destinationLocation;
    }

    public PathResult getRouteResult() {
        return routeResult;
    }

    public int getTravelTimeMinutes() {
        return travelTimeMinutes;
    }

    public LocalTime getPickupTime() {
        return pickupTime;
    }

    public int getHandoverMinutes() {
        return handoverMinutes;
    }

    public LocalTime getExpectedCompletionTime() {
        return expectedCompletionTime;
    }

    public synchronized AllocationStatus getStatus() {
        return status;
    }

    public synchronized void setStatus(AllocationStatus status) {
        this.status = status;
    }

    /**
     * Confirms the physical delivery of this allocation.
     * Enforces business constraints:
     * - Allocation must be in RESERVED state (cannot deliver without prior reservation)
     * - Cannot confirm an already DELIVERED allocation
     * - Cannot confirm a CANCELLED allocation
     * Updates batch delivered portions, sets vehicle current location to recipient location,
     * and marks vehicle available again.
     */
    public synchronized void confirmDelivery() {
        if (this.status == AllocationStatus.DELIVERED) {
            throw new InvalidAllocationException("Allocation " + allocationId + " is already marked DELIVERED.");
        }
        if (this.status == AllocationStatus.CANCELLED) {
            throw new InvalidAllocationException("Cannot confirm CANCELLED allocation " + allocationId + ".");
        }
        if (this.status != AllocationStatus.RESERVED) {
            throw new InvalidAllocationException("Cannot confirm delivery: Allocation " + allocationId 
                    + " must be in RESERVED state before delivery confirmation.");
        }

        // Commit portion changes on FoodBatch
        foodBatch.confirmDelivered(allocatedPortions);

        // Update vehicle state: moved to destination, free for new trips
        vehicle.setCurrentLocation(destinationLocation);
        vehicle.setAvailable(true);

        this.status = AllocationStatus.DELIVERED;
    }

    /**
     * Cancels a reserved allocation, rolling back reserved quantities and demand.
     */
    public synchronized void cancelAllocation() {
        if (this.status == AllocationStatus.DELIVERED) {
            throw new InvalidAllocationException("Cannot cancel an already DELIVERED allocation.");
        }
        if (this.status == AllocationStatus.CANCELLED) {
            return;
        }

        if (this.status == AllocationStatus.RESERVED) {
            foodBatch.releaseReservation(allocatedPortions);
            recipient.releaseDemand(allocatedPortions);
            vehicle.setAvailable(true);
        }

        this.status = AllocationStatus.CANCELLED;
    }

    public String getFormattedSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Allocation ID   : ").append(allocationId).append("\n");
        sb.append("Batch ID        : ").append(foodBatch.getBatchId()).append(" (").append(foodBatch.getCategory().name()).append(")\n");
        sb.append("Allocated       : ").append(allocatedPortions).append(" portions\n");
        sb.append("Donor / Pickup  : ").append(foodBatch.getDonor().getName()).append(" @ ").append(pickupLocation.getName()).append("\n");
        sb.append("Recipient       : ").append(recipient.getName()).append(" (").append(recipient.getRecipientType()).append(") @ ").append(destinationLocation.getName()).append("\n");
        sb.append("Vehicle         : ").append(vehicle.getName()).append(" [").append(vehicle.getVehicleId()).append("]\n");
        sb.append("Route           : ").append(routeResult.getFormattedRoute()).append("\n");
        sb.append("Travel Time     : ").append(travelTimeMinutes).append(" mins\n");
        sb.append("Pickup Time     : ").append(pickupTime.format(TIME_FMT)).append("\n");
        sb.append("Handover Time   : ").append(handoverMinutes).append(" mins\n");
        sb.append("Expected Done   : ").append(expectedCompletionTime.format(TIME_FMT)).append("\n");
        sb.append("Use-By Deadline : ").append(foodBatch.getDeclaredUseByTime().format(TIME_FMT)).append("\n");
        sb.append("Current Status  : ").append(status);
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("%-8s | Batch: %-5s | Recipient: %-15s | Qty: %2d | Vehicle: %-6s | Status: %-10s",
                allocationId, foodBatch.getBatchId(), recipient.getName(), allocatedPortions, vehicle.getVehicleId(), status);
    }
}
