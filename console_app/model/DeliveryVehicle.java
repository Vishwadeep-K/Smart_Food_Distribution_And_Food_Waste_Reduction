package model;

import graph.Location;

/**
 * Represents a distribution vehicle (van, electric loader, tempo) managed by Annam volunteers.
 */
public class DeliveryVehicle {
    private final String vehicleId;
    private final String name;
    private Location currentLocation;
    private final int portionCapacity;
    private boolean available;

    public DeliveryVehicle(String vehicleId, String name, Location currentLocation, int portionCapacity) {
        if (vehicleId == null || vehicleId.trim().isEmpty()) {
            throw new IllegalArgumentException("Vehicle ID cannot be null or empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Vehicle name cannot be null or empty.");
        }
        if (currentLocation == null) {
            throw new IllegalArgumentException("Current location cannot be null.");
        }
        if (portionCapacity <= 0) {
            throw new IllegalArgumentException("Portion capacity must be greater than zero.");
        }
        this.vehicleId = vehicleId.trim().toUpperCase();
        this.name = name.trim();
        this.currentLocation = currentLocation;
        this.portionCapacity = portionCapacity;
        this.available = true;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getName() {
        return name;
    }

    public synchronized Location getCurrentLocation() {
        return currentLocation;
    }

    public synchronized void setCurrentLocation(Location currentLocation) {
        if (currentLocation == null) {
            throw new IllegalArgumentException("Location cannot be null.");
        }
        this.currentLocation = currentLocation;
    }

    public int getPortionCapacity() {
        return portionCapacity;
    }

    public synchronized boolean isAvailable() {
        return available;
    }

    public synchronized void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {
        return name + " [" + vehicleId + "] @ " + currentLocation.getName() 
                + " | Capacity: " + portionCapacity + " portions | " + (available ? "AVAILABLE" : "BUSY");
    }
}
