package graph;

import java.util.*;

/**
 * Weighted Road Network implemented using an Adjacency List.
 * Represents physical connections and positive travel times between locations.
 */
public class RoadNetwork {
    private final Map<String, Location> locationMap;
    private final Map<String, List<Edge>> adjacencyList;

    public RoadNetwork() {
        this.locationMap = new LinkedHashMap<>();
        this.adjacencyList = new LinkedHashMap<>();
    }

    public void addLocation(Location location) {
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null.");
        }
        locationMap.put(location.getLocationId().toUpperCase(), location);
        adjacencyList.putIfAbsent(location.getLocationId().toUpperCase(), new ArrayList<>());
    }

    public void addDirectedRoad(String fromId, String toId, int travelTimeMinutes) {
        Location from = getLocation(fromId);
        Location to = getLocation(toId);
        if (from == null) {
            throw new IllegalArgumentException("Unknown source location ID: " + fromId);
        }
        if (to == null) {
            throw new IllegalArgumentException("Unknown destination location ID: " + toId);
        }
        adjacencyList.get(from.getLocationId().toUpperCase()).add(new Edge(to, travelTimeMinutes));
    }

    public void addBidirectionalRoad(String locId1, String locId2, int travelTimeMinutes) {
        addDirectedRoad(locId1, locId2, travelTimeMinutes);
        addDirectedRoad(locId2, locId1, travelTimeMinutes);
    }

    public Location getLocation(String locationId) {
        if (locationId == null) return null;
        return locationMap.get(locationId.trim().toUpperCase());
    }

    public Collection<Location> getAllLocations() {
        return Collections.unmodifiableCollection(locationMap.values());
    }

    public List<Edge> getNeighbors(Location location) {
        if (location == null) return Collections.emptyList();
        return Collections.unmodifiableList(
                adjacencyList.getOrDefault(location.getLocationId().toUpperCase(), Collections.emptyList())
        );
    }

    public int getLocationCount() {
        return locationMap.size();
    }

    public void printNetworkSummary() {
        System.out.println("==========================================================================");
        System.out.println("                       ROAD NETWORK TOPOLOGY                              ");
        System.out.println("==========================================================================");
        System.out.printf("%-10s | %-24s | %-16s | %s%n", "LOC ID", "NAME", "TYPE", "CONNECTED ROADS (TRAVEL TIME)");
        System.out.println("--------------------------------------------------------------------------");
        for (Location loc : locationMap.values()) {
            List<Edge> edges = adjacencyList.get(loc.getLocationId());
            StringBuilder sb = new StringBuilder();
            if (edges.isEmpty()) {
                sb.append("[No outgoing connections]");
            } else {
                for (int i = 0; i < edges.size(); i++) {
                    Edge e = edges.get(i);
                    sb.append(e.getDestination().getName()).append(" (").append(e.getTravelTimeMinutes()).append("m)");
                    if (i < edges.size() - 1) sb.append(", ");
                }
            }
            System.out.printf("%-10s | %-24s | %-16s | %s%n",
                    loc.getLocationId(), loc.getName(), loc.getAreaType(), sb.toString());
        }
        System.out.println("==========================================================================");
    }
}
