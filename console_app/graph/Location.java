package graph;

import java.util.Objects;

/**
 * Represents a geographical node (intersection, hub, donor facility, shelter, or community centre)
 * within the Annam distribution road network.
 */
public class Location {
    private final String locationId;
    private final String name;
    private final String areaType; // e.g., "Hub", "Restaurant Zone", "Shelter Area", "Transit Point"

    public Location(String locationId, String name, String areaType) {
        if (locationId == null || locationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Location ID cannot be null or empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Location name cannot be null or empty.");
        }
        this.locationId = locationId.trim().toUpperCase();
        this.name = name.trim();
        this.areaType = (areaType == null || areaType.trim().isEmpty()) ? "General" : areaType.trim();
    }

    public String getLocationId() {
        return locationId;
    }

    public String getName() {
        return name;
    }

    public String getAreaType() {
        return areaType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location other)) return false;
        return locationId.equalsIgnoreCase(other.locationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(locationId.toUpperCase());
    }

    @Override
    public String toString() {
        return name + " (" + locationId + ")";
    }
}
