package com.annam.policy;

import com.annam.graph.PathResult;
import java.time.LocalTime;

/**
 * Result returned by FoodMatchingPolicy evaluation.
 */
public class MatchResult {
    private final boolean accepted;
    private final String reason;
    private final int allocatedPortions;
    private final PathResult routeResult;
    private final LocalTime pickupTime;
    private final int travelTimeMinutes;
    private final int handoverMinutes;
    private final LocalTime expectedCompletionTime;
    private final LocalTime useByDeadline;

    private MatchResult(boolean accepted, String reason, int allocatedPortions, PathResult routeResult,
                        LocalTime pickupTime, int travelTimeMinutes, int handoverMinutes,
                        LocalTime expectedCompletionTime, LocalTime useByDeadline) {
        this.accepted = accepted;
        this.reason = reason;
        this.allocatedPortions = allocatedPortions;
        this.routeResult = routeResult;
        this.pickupTime = pickupTime;
        this.travelTimeMinutes = travelTimeMinutes;
        this.handoverMinutes = handoverMinutes;
        this.expectedCompletionTime = expectedCompletionTime;
        this.useByDeadline = useByDeadline;
    }

    public static MatchResult accept(int allocatedPortions, PathResult routeResult,
                                     LocalTime pickupTime, int travelTime, int handover,
                                     LocalTime expectedCompletion, LocalTime useByDeadline) {
        return new MatchResult(true, "Eligible match accepted within deadline.", allocatedPortions,
                routeResult, pickupTime, travelTime, handover, expectedCompletion, useByDeadline);
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

    public LocalTime getUseByDeadline() {
        return useByDeadline;
    }
}
