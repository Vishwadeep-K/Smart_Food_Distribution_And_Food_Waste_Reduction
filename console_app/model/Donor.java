package model;

import graph.Location;

/**
 * Represents an institution, restaurant, or cafeteria contributing surplus food.
 */
public class Donor {
    private final String donorId;
    private final String name;
    private final Location location;
    private final String contactPhone;

    public Donor(String donorId, String name, Location location, String contactPhone) {
        if (donorId == null || donorId.trim().isEmpty()) {
            throw new IllegalArgumentException("Donor ID cannot be null or empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Donor name cannot be null or empty.");
        }
        if (location == null) {
            throw new IllegalArgumentException("Donor location cannot be null.");
        }
        this.donorId = donorId.trim().toUpperCase();
        this.name = name.trim();
        this.location = location;
        this.contactPhone = contactPhone != null ? contactPhone.trim() : "N/A";
    }

    public String getDonorId() {
        return donorId;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    @Override
    public String toString() {
        return name + " [" + donorId + "] @ " + location.getName();
    }
}
