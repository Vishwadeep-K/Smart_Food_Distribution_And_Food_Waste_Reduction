package graph;

import java.util.*;

/**
 * Dijkstra's Shortest Path Algorithm implemented using a Min-Heap (PriorityQueue).
 * Computes the minimum travel time route across the RoadNetwork.
 */
public class DijkstraAlgorithm {

    /**
     * Internal node entry for the Min-Heap PriorityQueue.
     */
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

    /**
     * Computes the shortest travel-time path from source to destination using Dijkstra's algorithm.
     *
     * @param network     the road network
     * @param source      starting location
     * @param destination target location
     * @return PathResult with the reconstructed path and total travel time in minutes
     */
    public static PathResult findShortestPath(RoadNetwork network, Location source, Location destination) {
        if (network == null || source == null || destination == null) {
            return PathResult.unreachable(source, destination);
        }

        if (source.equals(destination)) {
            List<Location> singlePath = new ArrayList<>();
            singlePath.add(source);
            return PathResult.success(source, destination, singlePath, 0);
        }

        // Distance table
        Map<String, Integer> distances = new HashMap<>();
        // Predecessor table for reconstructing path
        Map<String, Location> predecessors = new HashMap<>();
        // Visited set
        Set<String> visited = new HashSet<>();

        // Initialize distances
        for (Location loc : network.getAllLocations()) {
            distances.put(loc.getLocationId(), Integer.MAX_VALUE);
        }
        distances.put(source.getLocationId(), 0);

        // Min-Heap priority queue
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

            // Optimization: if we have reached the destination, stop early
            if (currentLoc.equals(destination)) {
                break;
            }

            // Relax outgoing edges
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

        // Check if destination was reached
        int shortestTime = distances.getOrDefault(destination.getLocationId(), Integer.MAX_VALUE);
        if (shortestTime == Integer.MAX_VALUE) {
            return PathResult.unreachable(source, destination);
        }

        // Reconstruct path backwards from destination
        LinkedList<Location> reconstructedPath = new LinkedList<>();
        Location curr = destination;
        while (curr != null) {
            reconstructedPath.addFirst(curr);
            curr = predecessors.get(curr.getLocationId());
        }

        return PathResult.success(source, destination, reconstructedPath, shortestTime);
    }
}
