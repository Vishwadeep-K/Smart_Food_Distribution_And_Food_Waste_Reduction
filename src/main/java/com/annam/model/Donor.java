package com.annam.model;

import com.annam.graph.Location;
import java.util.Objects;

/**
 * Represents a surplus food donor (hotel, university canteen, bakery, cafeteria).
 */
public class Donor {
    private final String donorId;
    private String name;
    private String organization;
    private String donorType;
    private String phone;
    private String city;
    private Location location;

    public Donor(String donorId, String name, String organization, String phone, Location location) {
        this(donorId, name, organization, "Restaurant", phone, "", location);
    }

    public Donor(String donorId, String name, String organization, String donorType, String phone, String city, Location location) {
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
        this.organization = organization != null ? organization.trim() : name.trim();
        this.donorType = donorType != null && !donorType.trim().isEmpty() ? donorType.trim() : "Restaurant";
        this.phone = phone != null ? phone.trim() : "";
        this.city = city != null ? city.trim() : "";
        this.location = location;
    }

    public String getDonorId() {
        return donorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getDonorType() {
        return donorType;
    }

    public void setDonorType(String donorType) {
        this.donorType = donorType;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Donor other)) return false;
        return donorId.equalsIgnoreCase(other.donorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(donorId.toUpperCase());
    }

    @Override
    public String toString() {
        return name + " (" + donorId + ") @ " + location.getName();
    }
}
