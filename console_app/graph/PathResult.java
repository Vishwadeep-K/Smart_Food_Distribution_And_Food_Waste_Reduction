package graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Encapsulates the output of Dijkstra's shortest-path calculation.
 */
public class PathResult {
    private final Location source;
    private final Location destination;
    private final List<Location> path;
    private final int totalTravelTimeMinutes;
    private final boolean reachable;

    private PathResult(Location source, Location destination, List<Location> path, int totalTravelTimeMinutes, boolean reachable) {
        this.source = source;
        this.destination = destination;
        this.path = path != null ? Collections.unmodifiableList(new ArrayList<>(path)) : Collections.emptyList();
        this.totalTravelTimeMinutes = totalTravelTimeMinutes;
        this.reachable = reachable;
    }

    public static PathResult success(Location source, Location destination, List<Location> path, int totalTravelTimeMinutes) {
        return new PathResult(source, destination, path, totalTravelTimeMinutes, true);
    }

    public static PathResult unreachable(Location source, Location destination) {
        return new PathResult(source, destination, Collections.emptyList(), Integer.MAX_VALUE, false);
    }

    public Location getSource() {
        return source;
    }

    public Location getDestination() {
        return destination;
    }

    public List<Location> getPath() {
        return path;
    }

    public int getTotalTravelTimeMinutes() {
        return totalTravelTimeMinutes;
    }

    public boolean isReachable() {
        return reachable;
    }

    public String getFormattedRoute() {
        if (!reachable || path.isEmpty()) {
            return "No reachable route from " + (source != null ? source.getName() : "Unknown") 
                    + " to " + (destination != null ? destination.getName() : "Unknown");
        }
        return path.stream().map(Location::getName).collect(Collectors.joining(" -> "));
    }

    @Override
    public String toString() {
        return getFormattedRoute() + " (" + totalTravelTimeMinutes + " mins)";
    }
}
