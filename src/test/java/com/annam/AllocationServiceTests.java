package com.annam;

import com.annam.exception.InsufficientFoodException;
import com.annam.exception.InvalidAllocationException;
import com.annam.graph.Location;
import com.annam.graph.PathResult;
import com.annam.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AllocationServiceTests {

    private FoodBatch batch;
    private Recipient recipient;
    private DeliveryVehicle vehicle;
    private PathResult route;

    @BeforeEach
    void setUp() {
        Location locPickup = new Location("LOC01", "Central Hub", "Transit");
        Location locDest = new Location("LOC06", "East Ward", "Shelter");
        Donor donor = new Donor("D01", "Grand Hotel", "Hospitality", "123", locPickup);

        batch = new FoodBatch("B01", FoodCategory.MEALS, 30, LocalTime.of(14, 0), donor, locPickup, ApprovalStatus.APPROVED);
        recipient = new Shelter("R01", "Karuna Home", locDest, 50);
        vehicle = new DeliveryVehicle("V01", "Van-01", locPickup, 50, true);
        route = PathResult.success(locPickup, locDest, List.of(locPickup, locDest), 18);
    }

    @Test
    @DisplayName("TEST 6: Duplicate reservation attempt exceeding available balance is rejected")
    void test06_DuplicateReservationAttempt() {
        // Batch has 30 portions.
        // First reservation: 20 portions -> Success
        batch.reservePortions(20);
        assertEquals(10, batch.getAvailablePortions());
        assertEquals(20, batch.getReservedPortions());

        // Second attempt to reserve 20 portions (only 10 remaining) -> InsufficientFoodException
        assertThrows(InsufficientFoodException.class, () -> {
            batch.reservePortions(20);
        }, "Should strictly reject reservation exceeding available balance of 10");
    }

    @Test
    @DisplayName("TEST 13: Duplicate delivery confirmation is rejected")
    void test13_DuplicateDeliveryConfirmation() {
        FoodAllocation allocation = new FoodAllocation(
                "ALC0001", batch, recipient, vehicle, 20, route,
                LocalTime.of(10, 0), 10, AllocationStatus.RESERVED
        );
        batch.reservePortions(20);

        // First confirmation -> Success
        allocation.confirmDelivery();
        assertEquals(AllocationStatus.DELIVERED, allocation.getStatus());
        assertEquals(20, batch.getDeliveredPortions());
        assertEquals(0, batch.getReservedPortions());

        // Second confirmation attempt -> InvalidAllocationException
        assertThrows(InvalidAllocationException.class, () -> {
            allocation.confirmDelivery();
        }, "Cannot deliver an already DELIVERED allocation a second time");
    }

    @Test
    @DisplayName("Cannot confirm delivery on unreserved allocation")
    void testCannotDeliverUnreservedAllocation() {
        FoodAllocation allocation = new FoodAllocation(
                "ALC0002", batch, recipient, vehicle, 10, route,
                LocalTime.of(10, 0), 10, AllocationStatus.PROPOSED
        );

        assertThrows(InvalidAllocationException.class, () -> {
            allocation.confirmDelivery();
        }, "Allocation must be in RESERVED state before delivery confirmation");
    }
}
