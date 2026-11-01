package com.annam.model;

import com.annam.graph.Location;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Abstract base class representing a beneficiary organization (Shelter, Community Centre).
 * Demonstrates OOP Abstraction and Polymorphism.
 */
public abstract class Recipient {
    private final String recipientId;
    private String name;
    private Location location;
    private int totalRequiredPortions;
    private int remainingDemandPortions;
    protected final Set<FoodCategory> acceptedCategories;

    public Recipient(String recipientId, String name, Location location, int totalRequiredPortions) {
        if (recipientId == null || recipientId.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient ID cannot be null or empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient name cannot be null or empty.");
        }
        if (location == null) {
            throw new IllegalArgumentException("Recipient location cannot be null.");
        }
        if (totalRequiredPortions < 0) {
            throw new IllegalArgumentException("Required portions cannot be negative.");
        }
        this.recipientId = recipientId.trim().toUpperCase();
        this.name = name.trim();
        this.location = location;
        this.totalRequiredPortions = totalRequiredPortions;
        this.remainingDemandPortions = totalRequiredPortions;
        this.acceptedCategories = new HashSet<>();
    }

    public String getRecipientId() {
        return recipientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public int getTotalRequiredPortions() {
        return totalRequiredPortions;
    }

    public void setTotalRequiredPortions(int totalRequiredPortions) {
        this.totalRequiredPortions = totalRequiredPortions;
    }

    public synchronized int getRemainingDemandPortions() {
        return remainingDemandPortions;
    }

    public synchronized void setRemainingDemandPortions(int remainingDemandPortions) {
        this.remainingDemandPortions = Math.max(0, remainingDemandPortions);
    }

    public Set<FoodCategory> getAcceptedCategories() {
        return Collections.unmodifiableSet(acceptedCategories);
    }

    public synchronized void setAcceptedCategories(Set<FoodCategory> categories) {
        this.acceptedCategories.clear();
        if (categories != null) {
            this.acceptedCategories.addAll(categories);
        }
    }

    public synchronized void addAcceptedCategory(FoodCategory category) {
        if (category != null) {
            this.acceptedCategories.add(category);
        }
    }

    /**
     * Polymorphic method to determine whether this recipient accepts a specific food category.
     * Overridden by concrete subclasses Shelter and CommunityCentre.
     */
    public abstract boolean acceptsFoodCategory(FoodCategory category);

    /**
     * Identifies the recipient category (e.g. "Shelter", "Community Centre").
     */
    public abstract String getRecipientType();

    public synchronized void fulfillDemand(int portions) {
        if (portions <= 0) {
            throw new IllegalArgumentException("Portions fulfilled must be positive.");
        }
        if (portions > remainingDemandPortions) {
            throw new IllegalArgumentException("Cannot fulfill " + portions + " portions; only " 
                    + remainingDemandPortions + " portions remaining in demand.");
        }
        this.remainingDemandPortions -= portions;
    }

    public synchronized void releaseDemand(int portions) {
        if (portions <= 0) return;
        this.remainingDemandPortions = Math.min(totalRequiredPortions, this.remainingDemandPortions + portions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Recipient other)) return false;
        return recipientId.equalsIgnoreCase(other.recipientId);
    }

    @Override
    public int hashCode() {
        return recipientId.toUpperCase().hashCode();
    }

    @Override
    public String toString() {
        return name + " [" + recipientId + "] (" + getRecipientType() + ") @ " + location.getName()
                + " | Rem Demand: " + remainingDemandPortions + "/" + totalRequiredPortions;
    }
}
