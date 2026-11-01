package com.annam.policy;

import com.annam.graph.RoadNetwork;
import com.annam.model.DeliveryVehicle;
import com.annam.model.FoodBatch;
import com.annam.model.Recipient;
import java.time.LocalTime;

/**
 * Abstraction defining the contract for evaluating whether a surplus FoodBatch
 * can be allocated to a Recipient and transported by a DeliveryVehicle.
 */
public interface FoodMatchingPolicy {

    /**
     * Evaluates all domain, quantity, category, and deadline rules.
     *
     * @param batch        the surplus food batch under consideration
     * @param recipient    the candidate recipient organization
     * @param vehicle      the candidate delivery vehicle
     * @param roadNetwork  the road network for shortest path routing
     * @param currentTime  the current simulated time
     * @param handoverMins the fixed handover allowance in minutes
     * @return MatchResult indicating acceptance or rejection with detailed reasoning
     */
    MatchResult evaluateMatch(FoodBatch batch, Recipient recipient, DeliveryVehicle vehicle,
                              RoadNetwork roadNetwork, LocalTime currentTime, int handoverMins);
}
