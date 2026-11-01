package policy;

import graph.DijkstraAlgorithm;
import graph.PathResult;
import graph.RoadNetwork;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import model.DeliveryVehicle;
import model.FoodBatch;
import model.Recipient;

/**
 * Concrete implementation of FoodMatchingPolicy evaluating time-sensitive distribution rules.
 * Strictly checks:
 * 1. Batch existence
 * 2. Admin approval
 * 3. Expiry status against current simulated clock
 * 4. Recipient category compatibility (Polymorphism)
 * 5. Batch portion availability
 * 6. Recipient remaining demand
 * 7. Vehicle availability and capacity
 * 8. Pickup location validity
 * 9. Destination location validity
 * 10. Dijkstra shortest path feasibility
 * 11. Deadline condition: Pickup + Travel + Handover < Use-by deadline
 * 12. Non-zero portion allocation: min(available, demand, capacity)
 */
public class TimeSensitiveFoodMatchingPolicy implements FoodMatchingPolicy {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public MatchResult evaluateMatch(FoodBatch batch, Recipient recipient, DeliveryVehicle vehicle,
                                     RoadNetwork roadNetwork, LocalTime currentTime, int handoverMins) {
        // Condition 1: Batch exists
        if (batch == null) {
            return MatchResult.reject("Food batch record does not exist.");
        }

        // Condition 2: Batch is approved
        if (!batch.isApproved()) {
            return MatchResult.reject("Food batch " + batch.getBatchId() 
                    + " is NOT APPROVED by admin. Unapproved food cannot be distributed.");
        }

        // Condition 3: Batch is not expired
        if (batch.isExpired() || !currentTime.isBefore(batch.getDeclaredUseByTime())) {
            return MatchResult.reject("Food batch " + batch.getBatchId() 
                    + " is EXPIRED. (Current time: " + currentTime.format(TIME_FMT) 
                    + " >= Use-By: " + batch.getDeclaredUseByTime().format(TIME_FMT) + ")");
        }

        // Condition 4: Recipient accepts food category (Polymorphic call)
        if (recipient == null) {
            return MatchResult.reject("Recipient record does not exist.");
        }
        if (!recipient.acceptsFoodCategory(batch.getCategory())) {
            return MatchResult.reject("Category incompatible: " + recipient.getName() 
                    + " (" + recipient.getRecipientType() + ") does not accept " 
                    + batch.getCategory().getDisplayName() + ".");
        }

        // Condition 5: Batch has available portions
        if (batch.getAvailablePortions() <= 0) {
            return MatchResult.reject("Batch " + batch.getBatchId() + " has zero available portions.");
        }

        // Condition 6: Recipient still has demand
        if (recipient.getRemainingDemandPortions() <= 0) {
            return MatchResult.reject("Recipient " + recipient.getName() + " has zero remaining demand.");
        }

        // Condition 7: Vehicle capacity and availability
        if (vehicle == null) {
            return MatchResult.reject("No delivery vehicle assigned.");
        }
        if (!vehicle.isAvailable()) {
            return MatchResult.reject("Vehicle " + vehicle.getName() + " [" + vehicle.getVehicleId() + "] is currently BUSY.");
        }
        if (vehicle.getPortionCapacity() <= 0) {
            return MatchResult.reject("Vehicle " + vehicle.getName() + " has zero portion capacity.");
        }

        // Condition 8 & 9: Pickup and destination locations in RoadNetwork
        if (roadNetwork == null) {
            return MatchResult.reject("Road network configuration is missing.");
        }
        if (roadNetwork.getLocation(batch.getPickupLocation().getLocationId()) == null) {
            return MatchResult.reject("Pickup location " + batch.getPickupLocation().getName() + " not in road network.");
        }
        if (roadNetwork.getLocation(recipient.getLocation().getLocationId()) == null) {
            return MatchResult.reject("Recipient location " + recipient.getLocation().getName() + " not in road network.");
        }

        // Condition 10: Dijkstra shortest path calculation
        PathResult pathResult = DijkstraAlgorithm.findShortestPath(roadNetwork, batch.getPickupLocation(), recipient.getLocation());
        if (!pathResult.isReachable()) {
            return MatchResult.reject("No reachable road path between pickup (" + batch.getPickupLocation().getName() 
                    + ") and destination (" + recipient.getLocation().getName() + ").");
        }

        // Condition 11: Deadline feasibility check
        LocalTime pickupTime = currentTime;
        int travelTime = pathResult.getTotalTravelTimeMinutes();
        LocalTime expectedCompletion = pickupTime.plusMinutes((long) travelTime + handoverMins);

        // Strict rule: expected completion must finish strictly BEFORE declared use-by time
        if (!expectedCompletion.isBefore(batch.getDeclaredUseByTime())) {
            return MatchResult.reject("Delivery would finish after use-by deadline. Expected Completion: " 
                    + expectedCompletion.format(TIME_FMT) + " (Pickup " + pickupTime.format(TIME_FMT) 
                    + " + Travel " + travelTime + "m + Handover " + handoverMins + "m) >= Use-By " 
                    + batch.getDeclaredUseByTime().format(TIME_FMT) + ".");
        }

        // Condition 12: Quantity allocation calculation
        int allocatedPortions = Math.min(
                batch.getAvailablePortions(),
                Math.min(recipient.getRemainingDemandPortions(), vehicle.getPortionCapacity())
        );

        if (allocatedPortions <= 0) {
            return MatchResult.reject("Calculated allocation quantity is zero.");
        }

        return MatchResult.accept(allocatedPortions, pathResult, pickupTime, travelTime, handoverMins,
                expectedCompletion, batch.getDeclaredUseByTime());
    }
}
