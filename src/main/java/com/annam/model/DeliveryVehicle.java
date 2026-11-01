package com.annam.model;

import com.annam.graph.Location;
import java.util.Objects;

/**
 * Represents a transport vehicle coordinated by the Annam volunteer network.
 */
public class DeliveryVehicle {
    private final String vehicleId;
    private String name;
    private Location currentLocation;
    private int portionCapacity;
    private boolean available;

    public DeliveryVehicle(String vehicleId, String name, Location currentLocation, int portionCapacity) {
        this(vehicleId, name, currentLocation, portionCapacity, true);
    }

    public DeliveryVehicle(String vehicleId, String name, Location currentLocation, int portionCapacity, boolean available) {
        if (vehicleId == null || vehicleId.trim().isEmpty()) {
            throw new IllegalArgumentException("Vehicle ID cannot be null or empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Vehicle name cannot be null or empty.");
        }
        if (currentLocation == null) {
            throw new IllegalArgumentException("Vehicle location cannot be null.");
        }
        if (portionCapacity <= 0) {
            throw new IllegalArgumentException("Vehicle portion capacity must be positive (> 0).");
        }
        this.vehicleId = vehicleId.trim().toUpperCase();
        this.name = name.trim();
        this.currentLocation = currentLocation;
        this.portionCapacity = portionCapacity;
        this.available = available;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public synchronized void setCurrentLocation(Location currentLocation) {
        if (currentLocation == null) {
            throw new IllegalArgumentException("Current location cannot be null.");
        }
        this.currentLocation = currentLocation;
    }

    public int getPortionCapacity() {
        return portionCapacity;
    }

    public void setPortionCapacity(int portionCapacity) {
        if (portionCapacity <= 0) {
            throw new IllegalArgumentException("Portion capacity must be greater than zero.");
        }
        this.portionCapacity = portionCapacity;
    }

    public synchronized boolean isAvailable() {
        return available;
    }

    public synchronized void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeliveryVehicle other)) return false;
        return vehicleId.equalsIgnoreCase(other.vehicleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vehicleId.toUpperCase());
    }

    @Override
    public String toString() {
        return name + " [" + vehicleId + "] (Cap: " + portionCapacity + " portions) @ " 
                + currentLocation.getName() + " | " + (available ? "AVAILABLE" : "BUSY");
    }
}
