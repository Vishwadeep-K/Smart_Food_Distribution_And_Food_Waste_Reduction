package model;

import graph.Location;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Abstract base class representing a beneficiary organization (Shelter, Community Centre).
 * Demonstrates OOP Abstraction and Polymorphism.
 */
public abstract class Recipient {
    private final String recipientId;
    private final String name;
    private final Location location;
    private final int totalRequiredPortions;
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

    public Location getLocation() {
        return location;
    }

    public int getTotalRequiredPortions() {
        return totalRequiredPortions;
    }

    public synchronized int getRemainingDemandPortions() {
        return remainingDemandPortions;
    }

    public Set<FoodCategory> getAcceptedCategories() {
        return Collections.unmodifiableSet(acceptedCategories);
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

    /**
     * Reduces the remaining demand upon reservation or allocation.
     */
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

    /**
     * Restores demand if an allocation or reservation is cancelled.
     */
    public synchronized void releaseDemand(int portions) {
        if (portions <= 0) return;
        this.remainingDemandPortions = Math.min(totalRequiredPortions, this.remainingDemandPortions + portions);
    }

    @Override
    public String toString() {
        return name + " [" + recipientId + "] (" + getRecipientType() + ") @ " + location.getName()
                + " | Demand: " + remainingDemandPortions + "/" + totalRequiredPortions;
    }
}
