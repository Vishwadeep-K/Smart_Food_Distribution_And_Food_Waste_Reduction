package com.annam.graph;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DijkstraRoutingTests {

    private RoadNetwork network;
    private Location locA;
    private Location locB;
    private Location locC;
    private Location locD;

    @BeforeEach
    void setUp() {
        network = new RoadNetwork();
        locA = new Location("A", "Node A", "Transit");
        locB = new Location("B", "Node B", "Hub");
        locC = new Location("C", "Node C", "Shelter");
        locD = new Location("D", "Node D", "Market");

        network.addLocation(locA);
        network.addLocation(locB);
        network.addLocation(locC);
        network.addLocation(locD);

        // Path A -> B: 10 mins
        network.addBidirectionalRoad("A", "B", 10);
        // Path B -> C: 15 mins (Total A -> B -> C = 25 mins)
        network.addBidirectionalRoad("B", "C", 15);
        // Direct Path A -> C: 40 mins (Longer)
        network.addBidirectionalRoad("A", "C", 40);
        // Path C -> D: 5 mins
        network.addBidirectionalRoad("C", "D", 5);
    }

    @Test
    @DisplayName("Dijkstra chooses shortest cumulative path over longer direct route")
    void testShortestCumulativePath() {
        PathResult result = DijkstraAlgorithm.findShortestPath(network, locA, locC);

        assertTrue(result.isReachable());
        assertEquals(25, result.getTotalTravelTimeMinutes(), "Should take A -> B -> C (10+15=25) instead of direct A -> C (40)");
        assertEquals(3, result.getPath().size());
        assertEquals("Node A -> Node B -> Node C", result.getFormattedRoute());
    }

    @Test
    @DisplayName("Dijkstra multi-hop route traversal")
    void testMultiHopTraversal() {
        PathResult result = DijkstraAlgorithm.findShortestPath(network, locA, locD);

        assertTrue(result.isReachable());
        assertEquals(30, result.getTotalTravelTimeMinutes(), "A -> B -> C -> D = 10 + 15 + 5 = 30");
    }

    @Test
    @DisplayName("Dijkstra same source and destination")
    void testSameSourceAndDestination() {
        PathResult result = DijkstraAlgorithm.findShortestPath(network, locA, locA);

        assertTrue(result.isReachable());
        assertEquals(0, result.getTotalTravelTimeMinutes());
        assertEquals(1, result.getPath().size());
    }
}
