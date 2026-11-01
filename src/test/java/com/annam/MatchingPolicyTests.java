package com.annam;

import com.annam.graph.Location;
import com.annam.graph.RoadNetwork;
import com.annam.model.*;
import com.annam.policy.MatchResult;
import com.annam.policy.TimeSensitiveFoodMatchingPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

public class MatchingPolicyTests {

    private RoadNetwork roadNetwork;
    private TimeSensitiveFoodMatchingPolicy policy;
    private Donor donor;
    private Location locHub;
    private Location locEast;
    private Location locIsolated;
    private DeliveryVehicle vehicle;

    @BeforeEach
    void setUp() {
        policy = new TimeSensitiveFoodMatchingPolicy();
        roadNetwork = new RoadNetwork();

        locHub = new Location("LOC01", "Central Hub", "Transit");
        locEast = new Location("LOC06", "East Ward", "Residential");
        locIsolated = new Location("LOC99", "Isolated Island", "Remote");

        roadNetwork.addLocation(locHub);
        roadNetwork.addLocation(locEast);
        roadNetwork.addLocation(locIsolated);

        // 18 mins travel between Hub and East
        roadNetwork.addBidirectionalRoad("LOC01", "LOC06", 18);

        donor = new Donor("D01", "Grand Hotel", "Hospitality", "+91-98401", locHub);
        vehicle = new DeliveryVehicle("V01", "Van-01", locHub, 50, true);
    }

    @Test
    @DisplayName("TEST 1: Normal successful allocation within deadline")
    void test01_SuccessfulAllocation() {
        // Batch use-by 12:30. Current time 10:00.
        // Travel 18m + Handover 10m = 28m. Expected Completion 10:28 < 12:30.
        FoodBatch batch = new FoodBatch("B01", FoodCategory.MEALS, 40, LocalTime.of(12, 30), donor, locHub, ApprovalStatus.APPROVED);
        Recipient recipient = new Shelter("R01", "Shelter Home", locEast, 60);

        MatchResult res = policy.evaluateMatch(batch, recipient, vehicle, roadNetwork, LocalTime.of(10, 0), 10);

        assertTrue(res.isAccepted());
        assertEquals(40, res.getAllocatedPortions());
        assertEquals(18, res.getTravelTimeMinutes());
        assertEquals(LocalTime.of(10, 28), res.getExpectedCompletionTime());
    }

    @Test
    @DisplayName("TEST 2: Expired batch rejected")
    void test02_ExpiredBatch() {
        // Batch use-by 10:15. Current time 10:30 (Current time >= Use-by).
        FoodBatch batch = new FoodBatch("B02", FoodCategory.MEALS, 30, LocalTime.of(10, 15), donor, locHub, ApprovalStatus.APPROVED);
        Recipient recipient = new Shelter("R01", "Shelter Home", locEast, 50);

        MatchResult res = policy.evaluateMatch(batch, recipient, vehicle, roadNetwork, LocalTime.of(10, 30), 10);

        assertFalse(res.isAccepted());
        assertTrue(res.getReason().contains("EXPIRED"));
    }

    @Test
    @DisplayName("TEST 3: Delivery missing deadline rejected (Completion >= Use-By)")
    void test03_DeliveryMissingDeadline() {
        // Pickup 10:15 + Travel 18m + Handover 10m = Completion 10:43 >= Use-By 10:40
        FoodBatch batch = new FoodBatch("B03", FoodCategory.BIRYANI, 30, LocalTime.of(10, 40), donor, locHub, ApprovalStatus.APPROVED);
        Recipient recipient = new Shelter("R01", "Shelter Home", locEast, 50);

        MatchResult res = policy.evaluateMatch(batch, recipient, vehicle, roadNetwork, LocalTime.of(10, 15), 10);

        assertFalse(res.isAccepted());
        assertTrue(res.getReason().contains("finish after use-by deadline"));
    }

    @Test
    @DisplayName("TEST 4: Partial recipient demand (min of available, demand, vehicle capacity)")
    void test04_PartialRecipientDemand() {
        // Batch: 50 portions. Recipient demand: 20 portions. Vehicle cap: 50.
        FoodBatch batch = new FoodBatch("B04", FoodCategory.BREAD, 50, LocalTime.of(15, 0), donor, locHub, ApprovalStatus.APPROVED);
        Recipient recipient = new Shelter("R01", "Shelter Home", locEast, 20);

        MatchResult res = policy.evaluateMatch(batch, recipient, vehicle, roadNetwork, LocalTime.of(10, 0), 10);

        assertTrue(res.isAccepted());
        assertEquals(20, res.getAllocatedPortions(), "Should allocate minimum of batch (50), demand (20), vehicle (50)");
    }

    @Test
    @DisplayName("TEST 5: Vehicle capacity limitation capping allocation")
    void test05_VehicleCapacityLimitation() {
        // Batch: 40 portions. Recipient demand: 40 portions. Vehicle cap: 15.
        FoodBatch batch = new FoodBatch("B05", FoodCategory.MEALS, 40, LocalTime.of(14, 0), donor, locHub, ApprovalStatus.APPROVED);
        Recipient recipient = new Shelter("R01", "Shelter Home", locEast, 40);
        DeliveryVehicle smallVehicle = new DeliveryVehicle("V03", "Mini Cart", locHub, 15, true);

        MatchResult res = policy.evaluateMatch(batch, recipient, smallVehicle, roadNetwork, LocalTime.of(10, 0), 10);

        assertTrue(res.isAccepted());
        assertEquals(15, res.getAllocatedPortions(), "Should strictly cap at small vehicle capacity of 15");
    }

    @Test
    @DisplayName("TEST 7: Incompatible food category rejected via Polymorphism")
    void test07_IncompatibleCategory() {
        // CommunityCentre does not accept BIRYANI (Shelters do).
        FoodBatch batch = new FoodBatch("B07", FoodCategory.BIRYANI, 30, LocalTime.of(13, 0), donor, locHub, ApprovalStatus.APPROVED);
        Recipient communityCentre = new CommunityCentre("R03", "Hope Community Pantry", locEast, 40);

        MatchResult res = policy.evaluateMatch(batch, communityCentre, vehicle, roadNetwork, LocalTime.of(10, 0), 10);

        assertFalse(res.isAccepted());
        assertTrue(res.getReason().contains("Category incompatible"));
    }

    @Test
    @DisplayName("TEST 8: Unapproved food batch rejected")
    void test08_UnapprovedFood() {
        FoodBatch batch = new FoodBatch("B08", FoodCategory.MEALS, 30, LocalTime.of(14, 0), donor, locHub, ApprovalStatus.NOT_APPROVED);
        Recipient recipient = new Shelter("R01", "Shelter Home", locEast, 50);

        MatchResult res = policy.evaluateMatch(batch, recipient, vehicle, roadNetwork, LocalTime.of(10, 0), 10);

        assertFalse(res.isAccepted());
        assertTrue(res.getReason().contains("NOT APPROVED"));
    }

    @Test
    @DisplayName("TEST 15: Unreachable road location rejected")
    void test15_UnreachableRoadLocation() {
        FoodBatch batch = new FoodBatch("B15", FoodCategory.MEALS, 30, LocalTime.of(14, 0), donor, locHub, ApprovalStatus.APPROVED);
        Recipient remoteRecipient = new Shelter("R99", "Remote Home", locIsolated, 50);

        MatchResult res = policy.evaluateMatch(batch, remoteRecipient, vehicle, roadNetwork, LocalTime.of(10, 0), 10);

        assertFalse(res.isAccepted());
        assertTrue(res.getReason().contains("No reachable road path"));
    }
}
