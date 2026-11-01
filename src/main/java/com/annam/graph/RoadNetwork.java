package com.annam.graph;

import java.util.*;

/**
 * Custom weighted road network graph implemented using an adjacency list.
 * Demonstrates Graph Data Structure without third-party graph libraries.
 */
public class RoadNetwork {
    private final Map<String, Location> locations;
    private final Map<String, List<Edge>> adjacencyList;

    public RoadNetwork() {
        this.locations = new LinkedHashMap<>();
        this.adjacencyList = new LinkedHashMap<>();
    }

    public synchronized void addLocation(Location location) {
        if (location == null) return;
        locations.putIfAbsent(location.getLocationId().toUpperCase(), location);
        adjacencyList.putIfAbsent(location.getLocationId().toUpperCase(), new ArrayList<>());
    }

    public synchronized void addBidirectionalRoad(String locIdA, String locIdB, int travelTimeMinutes) {
        addDirectedRoad(locIdA, locIdB, travelTimeMinutes);
        addDirectedRoad(locIdB, locIdA, travelTimeMinutes);
    }

    public synchronized void addDirectedRoad(String fromId, String toId, int travelTimeMinutes) {
        if (fromId == null || toId == null) return;
        Location from = locations.get(fromId.toUpperCase());
        Location to = locations.get(toId.toUpperCase());

        if (from == null || to == null) {
            throw new IllegalArgumentException("Cannot add road: locations " + fromId + " or " + toId + " not found.");
        }

        adjacencyList.get(from.getLocationId()).add(new Edge(to, travelTimeMinutes));
    }

    public synchronized Location getLocation(String locationId) {
        if (locationId == null) return null;
        return locations.get(locationId.toUpperCase());
    }

    public synchronized boolean hasLocation(String locationId) {
        if (locationId == null) return false;
        return locations.containsKey(locationId.toUpperCase());
    }

    public synchronized List<Edge> getNeighbors(Location location) {
        if (location == null) return Collections.emptyList();
        return adjacencyList.getOrDefault(location.getLocationId().toUpperCase(), Collections.emptyList());
    }

    public synchronized Collection<Location> getAllLocations() {
        return Collections.unmodifiableCollection(locations.values());
    }

    public synchronized Map<String, List<Edge>> getAdjacencyList() {
        return Collections.unmodifiableMap(adjacencyList);
    }

    public synchronized int getLocationCount() {
        return locations.size();
    }

    public synchronized int getRoadCount() {
        int count = 0;
        for (List<Edge> edges : adjacencyList.values()) {
            count += edges.size();
        }
        return count;
    }
}
