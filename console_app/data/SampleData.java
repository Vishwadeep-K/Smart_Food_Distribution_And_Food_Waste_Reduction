package data;

import graph.Location;
import graph.RoadNetwork;
import java.time.LocalTime;
import model.*;
import service.FoodDistributionService;

/**
 * Preloads synthetic sample data for the Annam network:
 * - Exactly 5 Donors
 * - Exactly 4 Recipients (2 Shelters, 2 Community Centres)
 * - Exactly 3 Delivery Vehicles
 * - 12 Road Network Locations with positive travel time weights
 * - 22 Diverse Food Batches with varied categories, quantities, use-by times, and approval states.
 */
public class SampleData {

    public static void initializeSystem(FoodDistributionService service) {
        RoadNetwork net = service.getRoadNetwork();

        // ----------------------------------------------------
        // 1. Road Network: 12 Locations
        // ----------------------------------------------------
        Location loc01 = new Location("LOC01", "Central Hub", "Transit & Dispatch");
        Location loc02 = new Location("LOC02", "South Campus", "Educational Zone");
        Location loc03 = new Location("LOC03", "West Market", "Commercial Center");
        Location loc04 = new Location("LOC04", "North Gate", "Dining Corridor");
        Location loc05 = new Location("LOC05", "Tech Park", "Corporate Complex");
        Location loc06 = new Location("LOC06", "East Ward", "Residential & Shelter");
        Location loc07 = new Location("LOC07", "Riverside", "Care Home Enclave");
        Location loc08 = new Location("LOC08", "Old Town", "Community Hub");
        Location loc09 = new Location("LOC09", "Metro Cross", "Suburban Center");
        Location loc10 = new Location("LOC10", "Junction Alpha", "Way Point");
        Location loc11 = new Location("LOC11", "Junction Beta", "Way Point");
        Location loc12 = new Location("LOC12", "Express Ring", "Highway Connector");

        net.addLocation(loc01);
        net.addLocation(loc02);
        net.addLocation(loc03);
        net.addLocation(loc04);
        net.addLocation(loc05);
        net.addLocation(loc06);
        net.addLocation(loc07);
        net.addLocation(loc08);
        net.addLocation(loc09);
        net.addLocation(loc10);
        net.addLocation(loc11);
        net.addLocation(loc12);

        // Roads with positive travel times in minutes
        net.addBidirectionalRoad("LOC01", "LOC10", 8);
        net.addBidirectionalRoad("LOC10", "LOC04", 10);
        net.addBidirectionalRoad("LOC04", "LOC12", 12);
        net.addBidirectionalRoad("LOC12", "LOC05", 15);
        net.addBidirectionalRoad("LOC01", "LOC03", 14);
        net.addBidirectionalRoad("LOC03", "LOC11", 9);
        net.addBidirectionalRoad("LOC11", "LOC02", 11);
        net.addBidirectionalRoad("LOC01", "LOC06", 18);
        net.addBidirectionalRoad("LOC06", "LOC07", 15);
        net.addBidirectionalRoad("LOC07", "LOC08", 16);
        net.addBidirectionalRoad("LOC08", "LOC09", 12);
        net.addBidirectionalRoad("LOC10", "LOC06", 10);
        net.addBidirectionalRoad("LOC11", "LOC08", 14);
        net.addBidirectionalRoad("LOC05", "LOC07", 20);
        net.addBidirectionalRoad("LOC02", "LOC09", 22);
        net.addBidirectionalRoad("LOC12", "LOC09", 25);

        // ----------------------------------------------------
        // 2. Donors: Exactly 5 Donors
        // ----------------------------------------------------
        Donor d01 = new Donor("D01", "Grand Palace Hotel", loc01, "+91-98401-11001");
        Donor d02 = new Donor("D02", "Anna Univ Canteen", loc02, "+91-98401-11002");
        Donor d03 = new Donor("D03", "Green Leaf Bakery", loc03, "+91-98401-11003");
        Donor d04 = new Donor("D04", "Spice Garden Rest", loc04, "+91-98401-11004");
        Donor d05 = new Donor("D05", "Tech Park Cafeteria", loc05, "+91-98401-11005");

        service.registerDonor(d01);
        service.registerDonor(d02);
        service.registerDonor(d03);
        service.registerDonor(d04);
        service.registerDonor(d05);

        // ----------------------------------------------------
        // 3. Recipients: Exactly 4 Recipients
        // (Demonstrates Polymorphism via Shelter and CommunityCentre)
        // ----------------------------------------------------
        Recipient r01 = new Shelter("R01", "Karuna Children Home", loc06, 60);
        Recipient r02 = new Shelter("R02", "Mother Teresa Senior Home", loc07, 40);
        Recipient r03 = new CommunityCentre("R03", "Hope Community Centre", loc08, 50);
        Recipient r04 = new CommunityCentre("R04", "Peace Welfare Pantry", loc09, 35);

        service.registerRecipient(r01);
        service.registerRecipient(r02);
        service.registerRecipient(r03);
        service.registerRecipient(r04);

        // ----------------------------------------------------
        // 4. Vehicles: Exactly 3 Vehicles
        // ----------------------------------------------------
        DeliveryVehicle v01 = new DeliveryVehicle("V01", "Annam Van-01", loc01, 50);
        DeliveryVehicle v02 = new DeliveryVehicle("V02", "Annam Van-02", loc03, 30);
        DeliveryVehicle v03 = new DeliveryVehicle("V03", "Annam E-Loader-03", loc02, 20);

        service.registerVehicle(v01);
        service.registerVehicle(v02);
        service.registerVehicle(v03);

        // ----------------------------------------------------
        // 5. Food Batches: 22 Batches
        // ----------------------------------------------------
        // Format: ID, Category, Portions, Use-By, Donor, PickupLoc, Approval
        service.registerBatch(new FoodBatch("B01", FoodCategory.MEALS, 40, LocalTime.of(12, 30), d01, loc01, ApprovalStatus.APPROVED));
        service.registerBatch(new FoodBatch("B02", FoodCategory.BIRYANI, 30, LocalTime.of(11, 30), d04, loc04, ApprovalStatus.APPROVED));
        service.registerBatch(new FoodBatch("B03", FoodCategory.RICE, 25, LocalTime.of(13, 0), d02, loc02, ApprovalStatus.APPROVED));
        service.registerBatch(new FoodBatch("B04", FoodCategory.BREAD, 50, LocalTime.of(15, 30), d03, loc03, ApprovalStatus.APPROVED));
        service.registerBatch(new FoodBatch("B05", FoodCategory.FRUITS, 35, LocalTime.of(16, 0), d05, loc05, ApprovalStatus.APPROVED));
        service.registerBatch(new FoodBatch("B06", FoodCategory.VEGETABLES, 45, LocalTime.of(14, 30), d05, loc05, ApprovalStatus.APPROVED));
        service.registerBatch(new FoodBatch("B07", FoodCategory.CHAPATI, 30, LocalTime.of(12, 0), d01, loc01, ApprovalStatus.APPROVED));
        service.registerBatch(new FoodBatch("B08", FoodCategory.PACKED_FOOD, 60, LocalTime.of(17, 0), d03, loc03, ApprovalStatus.APPROVED));

        // Expired relative to clock advancement / test scenarios
        service.registerBatch(new FoodBatch("B09", FoodCategory.MEALS, 20, LocalTime.of(10, 20), d04, loc04, ApprovalStatus.APPROVED));
        
        // Tight deadline batch for testing travel time rejection
        service.registerBatch(new FoodBatch("B10", FoodCategory.BIRYANI, 35, LocalTime.of(10, 45), d01, loc01, ApprovalStatus.APPROVED));

        // Unapproved batches for testing approval gate
        service.registerBatch(new FoodBatch("B11", FoodCategory.RICE, 40, LocalTime.of(13, 30), d02, loc02, ApprovalStatus.NOT_APPROVED));
        service.registerBatch(new FoodBatch("B12", FoodCategory.CHAPATI, 25, LocalTime.of(14, 0), d04, loc04, ApprovalStatus.NOT_APPROVED));

        // Remaining approved batches with varying expiry times
        service.registerBatch(new FoodBatch("B13", FoodCategory.BREAD, 30, LocalTime.of(15, 0), d03, loc03, ApprovalStatus.APPROVED));
        // B14 shares the EXACT same use-by time (13:00) as B03 to demonstrate deterministic tie-breaker on Batch ID
        service.registerBatch(new FoodBatch("B14", FoodCategory.MEALS, 50, LocalTime.of(13, 0), d05, loc05, ApprovalStatus.APPROVED));
        service.registerBatch(new FoodBatch("B15", FoodCategory.PACKED_FOOD, 40, LocalTime.of(16, 30), d01, loc01, ApprovalStatus.APPROVED));
        service.registerBatch(new FoodBatch("B16", FoodCategory.VEGETABLES, 20, LocalTime.of(13, 45), d02, loc02, ApprovalStatus.APPROVED));
        service.registerBatch(new FoodBatch("B17", FoodCategory.FRUITS, 25, LocalTime.of(14, 15), d03, loc03, ApprovalStatus.APPROVED));
        service.registerBatch(new FoodBatch("B18", FoodCategory.BIRYANI, 15, LocalTime.of(12, 15), d04, loc04, ApprovalStatus.APPROVED));
        service.registerBatch(new FoodBatch("B19", FoodCategory.RICE, 30, LocalTime.of(14, 45), d05, loc05, ApprovalStatus.APPROVED));
        service.registerBatch(new FoodBatch("B20", FoodCategory.CHAPATI, 40, LocalTime.of(13, 15), d01, loc01, ApprovalStatus.APPROVED));
        service.registerBatch(new FoodBatch("B21", FoodCategory.MEALS, 35, LocalTime.of(15, 15), d02, loc02, ApprovalStatus.APPROVED));
        service.registerBatch(new FoodBatch("B22", FoodCategory.PACKED_FOOD, 50, LocalTime.of(18, 0), d03, loc03, ApprovalStatus.APPROVED));
    }
}
