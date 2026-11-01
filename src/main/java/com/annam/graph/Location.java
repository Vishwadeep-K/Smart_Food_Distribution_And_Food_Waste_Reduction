package com.annam.graph;

import java.util.Objects;

/**
 * Represents a node/vertex in the road network graph.
 */
public class Location {
    private final String locationId;
    private final String name;
    private final String zoneType;
    private final String description;

    public Location(String locationId, String name, String zoneType) {
        this(locationId, name, zoneType, "");
    }

    public Location(String locationId, String name, String zoneType, String description) {
        if (locationId == null || locationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Location ID cannot be null or empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Location name cannot be null or empty.");
        }
        this.locationId = locationId.trim().toUpperCase();
        this.name = name.trim();
        this.zoneType = zoneType != null ? zoneType.trim() : "General";
        this.description = description != null ? description.trim() : "";
    }

    public String getLocationId() {
        return locationId;
    }

    public String getName() {
        return name;
    }

    public String getZoneType() {
        return zoneType;
    }

    public String getDescription() {
        return description;
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
