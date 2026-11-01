package policy;

import graph.PathResult;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Encapsulates the outcome of matching a FoodBatch to a Recipient and DeliveryVehicle.
 */
public class MatchResult {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final boolean accepted;
    private final String reason;
    private final int allocatedPortions;
    private final PathResult routeResult;
    private final LocalTime pickupTime;
    private final int travelTimeMinutes;
    private final int handoverMinutes;
    private final LocalTime expectedCompletionTime;
    private final LocalTime useByTime;

    private MatchResult(boolean accepted, String reason, int allocatedPortions, PathResult routeResult,
                        LocalTime pickupTime, int travelTimeMinutes, int handoverMinutes,
                        LocalTime expectedCompletionTime, LocalTime useByTime) {
        this.accepted = accepted;
        this.reason = reason;
        this.allocatedPortions = allocatedPortions;
        this.routeResult = routeResult;
        this.pickupTime = pickupTime;
        this.travelTimeMinutes = travelTimeMinutes;
        this.handoverMinutes = handoverMinutes;
        this.expectedCompletionTime = expectedCompletionTime;
        this.useByTime = useByTime;
    }

    public static MatchResult accept(int allocatedPortions, PathResult routeResult, LocalTime pickupTime,
                                     int travelTimeMinutes, int handoverMinutes,
                                     LocalTime expectedCompletionTime, LocalTime useByTime) {
        return new MatchResult(true, "Match criteria fully satisfied.", allocatedPortions,
                routeResult, pickupTime, travelTimeMinutes, handoverMinutes, expectedCompletionTime, useByTime);
    }

    public static MatchResult reject(String reason) {
        return new MatchResult(false, reason, 0, null, null, 0, 0, null, null);
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String getReason() {
        return reason;
    }

    public int getAllocatedPortions() {
        return allocatedPortions;
    }

    public PathResult getRouteResult() {
        return routeResult;
    }

    public LocalTime getPickupTime() {
        return pickupTime;
    }

    public int getTravelTimeMinutes() {
        return travelTimeMinutes;
    }

    public int getHandoverMinutes() {
        return handoverMinutes;
    }

    public LocalTime getExpectedCompletionTime() {
        return expectedCompletionTime;
    }

    public LocalTime getUseByTime() {
        return useByTime;
    }

    public String getFormattedDetails() {
        StringBuilder sb = new StringBuilder();
        if (accepted) {
            sb.append("STATUS         : MATCH ACCEPTED\n");
            sb.append("Allocated Qty  : ").append(allocatedPortions).append(" portions\n");
            sb.append("Route          : ").append(routeResult.getFormattedRoute()).append("\n");
            sb.append("Travel Time    : ").append(travelTimeMinutes).append(" minutes\n");
            sb.append("Pickup Time    : ").append(pickupTime.format(TIME_FMT)).append("\n");
            sb.append("Handover Time  : ").append(handoverMinutes).append(" minutes\n");
            sb.append("Expected Done  : ").append(expectedCompletionTime.format(TIME_FMT)).append("\n");
            sb.append("Use-By Limit   : ").append(useByTime.format(TIME_FMT)).append(" (Feasible: Completion < Use-By)");
        } else {
            sb.append("STATUS         : MATCH REJECTED\n");
            sb.append("Reason         : ").append(reason);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return accepted ? "MATCH ACCEPTED (" + allocatedPortions + " portions)" : "MATCH REJECTED: " + reason;
    }
}
