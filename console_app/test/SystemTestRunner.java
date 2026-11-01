package test;

import data.SampleData;
import exception.AnnamException;
import exception.DuplicateReservationException;
import exception.FoodExpiredException;
import exception.InvalidAllocationException;
import graph.Location;
import graph.RoadNetwork;
import java.time.LocalTime;
import java.util.List;
import java.util.PriorityQueue;
import model.*;
import policy.MatchResult;
import policy.TimeSensitiveFoodMatchingPolicy;
import service.FoodDistributionService;
import util.SimulatedClock;

/**
 * Automated test runner covering all 10 required test scenarios.
 * Validates business constraints, data structure operations, Dijkstra feasibility,
 * and edge cases.
 */
public class SystemTestRunner {

    private int testsPassed = 0;
    private int totalTests = 0;

    public static void main(String[] args) {
        SystemTestRunner runner = new SystemTestRunner();
        runner.runAllTests();
    }

    public void runAllTests() {
        System.out.println("==========================================================================");
        System.out.println("                 ANNAM SYSTEM AUTOMATED TEST SUITE                        ");
        System.out.println("==========================================================================");
        testsPassed = 0;
        totalTests = 0;

        test01_NormalSuccessfulAllocation();
        test02_ExpiredBatchRejection();
        test03_TripMissingDeadlineRejection();
        test04_PartialDemandAllocationAndRemainder();
        test05_VehicleCapacityLimitation();
        test06_DuplicateReservationAttempt();
        test07_IncompatibleCategoryRejection();
        test08_UnapprovedBatchRejection();
        test09_PriorityOrderingEarliestUseByFirst();
        test10_EqualPriorityTieBreaker();

        System.out.println("==========================================================================");
        System.out.printf("TEST SUITE SUMMARY: %d / %d TESTS PASSED%n", testsPassed, totalTests);
        if (testsPassed == totalTests) {
            System.out.println("ALL TEST CASES PASSED SUCCESSFULLY!");
        } else {
            System.out.println("SOME TESTS FAILED. PLEASE REVIEW LOGS ABOVE.");
        }
        System.out.println("==========================================================================");
    }

    private void recordResult(String testId, String testName, boolean passed, String details) {
        totalTests++;
        if (passed) {
            testsPassed++;
            System.out.printf("[PASS] %-8s: %-38s | %s%n", testId, testName, details);
        } else {
            System.out.printf("[FAIL] %-8s: %-38s | %s%n", testId, testName, details);
        }
    }

    /**
     * TEST 1: Normal successful allocation.
     * Food is compatible, quantity available, vehicle capacity sufficient, delivery meets deadline.
     */
    public void test01_NormalSuccessfulAllocation() {
        FoodDistributionService svc = createTestService(LocalTime.of(10, 0));
        // Batch B01: MEALS, 40 portions, Use-by 12:30 @ Central Hub (LOC01)
        // Recipient R01: Shelter @ East Ward (LOC06), accepts MEALS, demand 60
        // Vehicle V01: Cap 50 @ Central Hub (LOC01)
        // Travel LOC01 -> LOC10 -> LOC06 = 18 mins. Handover = 10 mins. Done = 10:28 < 12:30.
        MatchResult result = svc.simulateMatch("B01", "R01", "V01");
        boolean passed = result.isAccepted() && result.getAllocatedPortions() == 40;
        recordResult("TEST 1", "Normal Successful Allocation", passed,
                "Allocated 40 portions. Completion: " + result.getExpectedCompletionTime() + " < Use-By: 12:30");
    }

    /**
     * TEST 2: Expired batch.
     * Batch becomes unavailable and cannot be allocated.
     */
    public void test02_ExpiredBatchRejection() {
        FoodDistributionService svc = createTestService(LocalTime.of(10, 30));
        // Batch B09 has use-by time 10:20. At 10:30, it is expired.
        svc.runExpiryCheck();
        FoodBatch b09 = svc.getBatch("B09");
        boolean isExpiredState = b09.isExpired();

        MatchResult result = svc.simulateMatch("B09", "R01", "V01");
        boolean passed = isExpiredState && !result.isAccepted() && result.getReason().contains("EXPIRED");
        recordResult("TEST 2", "Expired Batch Rejection", passed,
                "Batch marked expired. Match rejected: " + result.getReason());
    }

    /**
     * TEST 3: Trip missing its deadline.
     * Match rejected because pickup + travel + handover finishes after use-by time.
     */
    public void test03_TripMissingDeadlineRejection() {
        FoodDistributionService svc = createTestService(LocalTime.of(10, 0));
        // Batch B10: BIRYANI @ LOC01, Use-by 10:45
        // Recipient R04: Peace Welfare @ LOC09 (Metro Cross).
        // Shortest route from LOC01 to LOC09 takes 44 mins!
        // Pickup 10:00 + Travel 44m + Handover 10m = Completion 10:54 > 10:45.
        // Wait, R04 is CommunityCentre which doesn't accept Biryani, so test with R02 (Shelter @ LOC07 Riverside).
        // Route LOC01 -> LOC10 (8m) -> LOC06 (10m) -> LOC07 (15m) = 33 mins travel.
        // Pickup 10:15 (set clock to 10:15):
        // 10:15 + 33 mins travel + 10 mins handover = 10:58 Completion > 10:45 Use-by!
        svc.getClock().setTime(LocalTime.of(10, 15));
        MatchResult result = svc.simulateMatch("B10", "R02", "V01");
        boolean passed = !result.isAccepted() && result.getReason().contains("deadline");
        recordResult("TEST 3", "Trip Missing Deadline Rejection", passed,
                "Rejected on deadline violation: " + result.getReason());
    }

    /**
     * TEST 4: Partial demand.
     * Food = 50 portions, Recipient demand = 20, Vehicle capacity = 50.
     * Expected: Only 20 portions allocated. Remaining 30 portions retained.
     */
    public void test04_PartialDemandAllocationAndRemainder() {
        RoadNetwork net = new RoadNetwork();
        Location l1 = new Location("L1", "Loc A", "Hub");
        Location l2 = new Location("L2", "Loc B", "Zone");
        net.addLocation(l1);
        net.addLocation(l2);
        net.addBidirectionalRoad("L1", "L2", 10);

        Donor donor = new Donor("TD1", "Test Donor", l1, "123");
        FoodBatch batch = new FoodBatch("TB4", FoodCategory.MEALS, 50, LocalTime.of(14, 0), donor, l1, ApprovalStatus.APPROVED);
        Recipient recipient = new Shelter("TR4", "Test Shelter", l2, 20);
        DeliveryVehicle vehicle = new DeliveryVehicle("TV4", "Test Van", l1, 50);

        FoodDistributionService svc = new FoodDistributionService(net, new SimulatedClock(LocalTime.of(10, 0), 10));
        svc.registerBatch(batch);
        svc.registerRecipient(recipient);
        svc.registerVehicle(vehicle);

        FoodAllocation alloc = svc.createAllocation("TB4", "TR4", "TV4");
        svc.reserveAllocation(alloc);

        boolean allocated20 = alloc.getAllocatedPortions() == 20;
        boolean remaining30 = batch.getAvailablePortions() == 30;
        boolean demandZero = recipient.getRemainingDemandPortions() == 0;
        boolean remainderTracked = svc.getUnallocatedRemainders().contains(batch);

        boolean passed = allocated20 && remaining30 && demandZero && remainderTracked;
        recordResult("TEST 4", "Partial Demand & Remainder Retained", passed,
                "Allocated: 20 | Remainder available: " + batch.getAvailablePortions() + " | In Remainder List: " + remainderTracked);
    }

    /**
     * TEST 5: Vehicle capacity limitation.
     * Food = 40, Demand = 40, Vehicle capacity = 15.
     * Expected: Only 15 portions allocated.
     */
    public void test05_VehicleCapacityLimitation() {
        RoadNetwork net = new RoadNetwork();
        Location l1 = new Location("L1", "Loc A", "Hub");
        Location l2 = new Location("L2", "Loc B", "Zone");
        net.addLocation(l1);
        net.addLocation(l2);
        net.addBidirectionalRoad("L1", "L2", 10);

        Donor donor = new Donor("TD5", "Test Donor", l1, "123");
        FoodBatch batch = new FoodBatch("TB5", FoodCategory.BREAD, 40, LocalTime.of(15, 0), donor, l1, ApprovalStatus.APPROVED);
        Recipient recipient = new CommunityCentre("TR5", "Test Centre", l2, 40);
        DeliveryVehicle vehicle = new DeliveryVehicle("TV5", "Small Loader", l1, 15);

        FoodDistributionService svc = new FoodDistributionService(net, new SimulatedClock(LocalTime.of(10, 0), 10));
        svc.registerBatch(batch);
        svc.registerRecipient(recipient);
        svc.registerVehicle(vehicle);

        FoodAllocation alloc = svc.createAllocation("TB5", "TR5", "TV5");
        boolean passed = alloc.getAllocatedPortions() == 15;
        recordResult("TEST 5", "Vehicle Capacity Limitation", passed,
                "Allocation strictly capped at vehicle capacity of 15 (Batch: 40, Demand: 40).");
    }

    /**
     * TEST 6: Duplicate reservation attempt.
     * Second reservation exceeding remaining available quantity is rejected.
     */
    public void test06_DuplicateReservationAttempt() {
        RoadNetwork net = new RoadNetwork();
        Location l1 = new Location("L1", "Loc A", "Hub");
        Location l2 = new Location("L2", "Loc B", "Zone");
        net.addLocation(l1);
        net.addLocation(l2);
        net.addBidirectionalRoad("L1", "L2", 10);

        Donor donor = new Donor("TD6", "Test Donor", l1, "123");
        FoodBatch batch = new FoodBatch("TB6", FoodCategory.MEALS, 30, LocalTime.of(15, 0), donor, l1, ApprovalStatus.APPROVED);
        Recipient r1 = new Shelter("TR6A", "Shelter A", l2, 20);
        Recipient r2 = new Shelter("TR6B", "Shelter B", l2, 20);
        DeliveryVehicle v1 = new DeliveryVehicle("TV6A", "Van 1", l1, 50);
        DeliveryVehicle v2 = new DeliveryVehicle("TV6B", "Van 2", l1, 50);

        FoodDistributionService svc = new FoodDistributionService(net, new SimulatedClock(LocalTime.of(10, 0), 10));
        svc.registerBatch(batch);
        svc.registerRecipient(r1);
        svc.registerRecipient(r2);
        svc.registerVehicle(v1);
        svc.registerVehicle(v2);

        // First reservation: 20 portions out of 30 -> succeeds, leaving 10 available
        FoodAllocation alloc1 = svc.createAllocation("TB6", "TR6A", "TV6A");
        svc.reserveAllocation(alloc1);

        // Attempting to reserve allocation 1 again must trigger DuplicateReservationException
        boolean caughtDuplicate = false;
        try {
            svc.reserveAllocation(alloc1);
        } catch (DuplicateReservationException e) {
            caughtDuplicate = true;
        }

        // Second recipient asks for 20 portions, but only 10 remain available in TB6
        // When evaluating match, feasible quantity should only be 10, preventing over-reservation
        MatchResult match2 = svc.simulateMatch("TB6", "TR6B", "TV6B");
        boolean quantityLimited = match2.isAccepted() && match2.getAllocatedPortions() == 10;

        boolean passed = caughtDuplicate && quantityLimited && batch.getAvailablePortions() == 10;
        recordResult("TEST 6", "Duplicate Reservation & Over-allocation Prevention", passed,
                "Duplicate reservation prevented; second request strictly limited to remaining 10 portions.");
    }

    /**
     * TEST 7: Incompatible category.
     * Recipient does not accept the food category -> Match rejected.
     */
    public void test07_IncompatibleCategoryRejection() {
        FoodDistributionService svc = createTestService(LocalTime.of(10, 0));
        // Batch B02 is BIRYANI.
        // Recipient R03 is Hope Community Centre, which accepts FRUITS, VEGETABLES, PACKED_FOOD, BREAD, MEALS (NOT Biryani).
        MatchResult result = svc.simulateMatch("B02", "R03", "V01");
        boolean passed = !result.isAccepted() && result.getReason().contains("incompatible");
        recordResult("TEST 7", "Incompatible Category Rejection", passed,
                "Polymorphic check rejected match: " + result.getReason());
    }

    /**
     * TEST 8: Unapproved batch.
     * Batch is not approved -> Match rejected.
     */
    public void test08_UnapprovedBatchRejection() {
        FoodDistributionService svc = createTestService(LocalTime.of(10, 0));
        // Batch B11 is NOT_APPROVED
        MatchResult result = svc.simulateMatch("B11", "R01", "V01");
        boolean passed = !result.isAccepted() && result.getReason().contains("NOT APPROVED");
        recordResult("TEST 8", "Unapproved Batch Rejection", passed,
                "Unapproved food rejected: " + result.getReason());
    }

    /**
     * TEST 9: Priority ordering.
     * Multiple approved batches with different use-by times. Earliest use-by is polled first.
     */
    public void test09_PriorityOrderingEarliestUseByFirst() {
        PriorityQueue<FoodBatch> queue = new PriorityQueue<>();
        Donor d = new Donor("D", "D", new Location("L", "L", "T"), "0");

        FoodBatch b1 = new FoodBatch("B01", FoodCategory.MEALS, 10, LocalTime.of(14, 0), d, d.getLocation(), ApprovalStatus.APPROVED);
        FoodBatch b2 = new FoodBatch("B02", FoodCategory.MEALS, 10, LocalTime.of(12, 30), d, d.getLocation(), ApprovalStatus.APPROVED);
        FoodBatch b3 = new FoodBatch("B03", FoodCategory.MEALS, 10, LocalTime.of(13, 15), d, d.getLocation(), ApprovalStatus.APPROVED);

        queue.add(b1);
        queue.add(b2);
        queue.add(b3);

        FoodBatch first = queue.poll();
        FoodBatch second = queue.poll();
        FoodBatch third = queue.poll();

        boolean orderCorrect = "B02".equals(first.getBatchId()) 
                && "B03".equals(second.getBatchId()) 
                && "B01".equals(third.getBatchId());

        recordResult("TEST 9", "Priority Ordering (Earliest Use-By First)", orderCorrect,
                "Order: " + first.getBatchId() + " (12:30) -> " + second.getBatchId() + " (13:15) -> " + third.getBatchId() + " (14:00)");
    }

    /**
     * TEST 10: Equal priority tie-breaker.
     * Two batches with same use-by time. Resolved deterministically by Batch ID.
     */
    public void test10_EqualPriorityTieBreaker() {
        PriorityQueue<FoodBatch> queue = new PriorityQueue<>();
        Donor d = new Donor("D", "D", new Location("L", "L", "T"), "0");

        // Both batches have exactly 13:00 use-by time
        FoodBatch bZ = new FoodBatch("BZ", FoodCategory.MEALS, 10, LocalTime.of(13, 0), d, d.getLocation(), ApprovalStatus.APPROVED);
        FoodBatch bA = new FoodBatch("BA", FoodCategory.MEALS, 10, LocalTime.of(13, 0), d, d.getLocation(), ApprovalStatus.APPROVED);

        queue.add(bZ);
        queue.add(bA);

        FoodBatch first = queue.poll();
        FoodBatch second = queue.poll();

        boolean passed = "BA".equals(first.getBatchId()) && "BZ".equals(second.getBatchId());
        recordResult("TEST 10", "Equal Priority Tie Resolved by Batch ID", passed,
                "Deterministic order for 13:00: " + first.getBatchId() + " followed by " + second.getBatchId());
    }

    private FoodDistributionService createTestService(LocalTime clockTime) {
        SimulatedClock clock = new SimulatedClock(clockTime, 10);
        FoodDistributionService svc = new FoodDistributionService(new RoadNetwork(), clock);
        SampleData.initializeSystem(svc);
        return svc;
    }
}
