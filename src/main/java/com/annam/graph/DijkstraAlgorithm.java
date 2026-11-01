package com.annam.graph;

import java.util.*;

/**
 * Dijkstra's Shortest Path Algorithm implemented using a Min-Heap (Java PriorityQueue).
 * Demonstrates classical greedy graph traversal algorithm.
 */
public class DijkstraAlgorithm {

    private static class NodeDistance implements Comparable<NodeDistance> {
        final Location location;
        final int distance;

        NodeDistance(Location location, int distance) {
            this.location = location;
            this.distance = distance;
        }

        @Override
        public int compareTo(NodeDistance other) {
            int cmp = Integer.compare(this.distance, other.distance);
            if (cmp != 0) return cmp;
            // Deterministic tie-breaker
            return this.location.getLocationId().compareTo(other.location.getLocationId());
        }
    }

    public static PathResult findShortestPath(RoadNetwork network, Location source, Location destination) {
        if (network == null || source == null || destination == null) {
            return PathResult.unreachable(source, destination);
        }

        if (source.equals(destination)) {
            List<Location> singlePath = new ArrayList<>();
            singlePath.add(source);
            return PathResult.success(source, destination, singlePath, 0);
        }

        Map<String, Integer> distances = new HashMap<>();
        Map<String, Location> predecessors = new HashMap<>();
        Set<String> visited = new HashSet<>();

        for (Location loc : network.getAllLocations()) {
            distances.put(loc.getLocationId(), Integer.MAX_VALUE);
        }
        distances.put(source.getLocationId(), 0);

        PriorityQueue<NodeDistance> minHeap = new PriorityQueue<>();
        minHeap.add(new NodeDistance(source, 0));

        while (!minHeap.isEmpty()) {
            NodeDistance current = minHeap.poll();
            Location currentLoc = current.location;
            String currentId = currentLoc.getLocationId();

            if (visited.contains(currentId)) {
                continue;
            }
            visited.add(currentId);

            if (currentLoc.equals(destination)) {
                break;
            }

            for (Edge edge : network.getNeighbors(currentLoc)) {
                Location neighbor = edge.getDestination();
                String neighborId = neighbor.getLocationId();

                if (visited.contains(neighborId)) {
                    continue;
                }

                int currentDist = distances.get(currentId);
                int edgeWeight = edge.getTravelTimeMinutes();
                int newDist = currentDist + edgeWeight;

                if (newDist < distances.getOrDefault(neighborId, Integer.MAX_VALUE)) {
                    distances.put(neighborId, newDist);
                    predecessors.put(neighborId, currentLoc);
                    minHeap.add(new NodeDistance(neighbor, newDist));
                }
            }
        }

        int shortestTime = distances.getOrDefault(destination.getLocationId(), Integer.MAX_VALUE);
        if (shortestTime == Integer.MAX_VALUE) {
            return PathResult.unreachable(source, destination);
        }

        LinkedList<Location> reconstructedPath = new LinkedList<>();
        Location curr = destination;
        while (curr != null) {
            reconstructedPath.addFirst(curr);
            curr = predecessors.get(curr.getLocationId());
        }

        return PathResult.success(source, destination, reconstructedPath, shortestTime);
    }
}
