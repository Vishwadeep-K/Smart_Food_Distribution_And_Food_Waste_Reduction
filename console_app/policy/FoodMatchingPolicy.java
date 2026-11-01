package policy;

import graph.RoadNetwork;
import java.time.LocalTime;
import model.DeliveryVehicle;
import model.FoodBatch;
import model.Recipient;

/**
 * Strategy abstraction defining rules to determine if a FoodBatch can be matched
 * with a Recipient and transported by a DeliveryVehicle.
 */
public interface FoodMatchingPolicy {

    /**
     * Evaluates whether a proposed match between batch, recipient, and vehicle is valid and feasible.
     *
     * @param batch        the surplus food batch
     * @param recipient    the recipient organization
     * @param vehicle      the candidate delivery vehicle
     * @param roadNetwork  the road network for Dijkstra routing
     * @param currentTime  the simulated time of request
     * @param handoverMins the fixed handover allowance in minutes
     * @return MatchResult containing accepted/rejected status, reasons, quantity, and route
     */
    MatchResult evaluateMatch(FoodBatch batch, Recipient recipient, DeliveryVehicle vehicle,
                              RoadNetwork roadNetwork, LocalTime currentTime, int handoverMins);
}
