package graph;

/**
 * Represents a directed connection between two locations with an edge weight
 * representing positive travel time in minutes.
 */
public class Edge {
    private final Location destination;
    private final int travelTimeMinutes;

    public Edge(Location destination, int travelTimeMinutes) {
        if (destination == null) {
            throw new IllegalArgumentException("Destination location cannot be null.");
        }
        if (travelTimeMinutes <= 0) {
            throw new IllegalArgumentException("Travel time must be strictly positive (> 0 minutes).");
        }
        this.destination = destination;
        this.travelTimeMinutes = travelTimeMinutes;
    }

    public Location getDestination() {
        return destination;
    }

    public int getTravelTimeMinutes() {
        return travelTimeMinutes;
    }

    @Override
    public String toString() {
        return "--> " + destination.getName() + " [" + travelTimeMinutes + " mins]";
    }
}
