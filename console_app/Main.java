import data.SampleData;
import exception.AnnamException;
import graph.DijkstraAlgorithm;
import graph.Location;
import graph.PathResult;
import graph.RoadNetwork;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import model.*;
import policy.MatchResult;
import service.FoodDistributionService;
import test.SystemTestRunner;
import util.SimulatedClock;

/**
 * Main entry point for the Annam Food Distribution & Waste Reduction System.
 * Menu-driven interactive console application demonstrating Java OOP, Collections,
 * Dijkstra's shortest-path algorithm, and time-sensitive scheduling.
 */
public class Main {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Scanner scanner = new Scanner(System.in);
    private static FoodDistributionService service;

    public static void main(String[] args) {
        // Initialize system components
        RoadNetwork roadNetwork = new RoadNetwork();
        SimulatedClock clock = new SimulatedClock(LocalTime.of(10, 0), 10);
        service = new FoodDistributionService(roadNetwork, clock);

        // Preload synthetic sample data
        SampleData.initializeSystem(service);

        boolean running = true;
        printWelcomeBanner();

        while (running) {
            printMainMenu();
            String choice = prompt("Enter your choice (1-19): ");

            switch (choice.trim()) {
                case "1" -> viewCurrentSimulatedTime();
                case "2" -> changeSimulatedTime();
                case "3" -> viewFoodBatches();
                case "4" -> viewApprovedFoodPriorityQueue();
                case "5" -> approveFoodBatch();
                case "6" -> viewRecipients();
                case "7" -> viewRecipientDemand();
                case "8" -> viewVehicles();
                case "9" -> createFoodAllocation();
                case "10" -> reserveFood();
                case "11" -> confirmDelivery();
                case "12" -> viewAllocationHistory();
                case "13" -> viewUnallocatedRemainders();
                case "14" -> runExpiryCheck();
                case "15" -> viewRoadNetwork();
                case "16" -> findShortestRoute();
                case "17" -> viewSystemSummary();
                case "18" -> runTestScenarios();
                case "19" -> {
                    System.out.println("\nThank you for using Annam Food Distribution System. Goodbye!");
                    running = false;
                }
                default -> System.out.println("\n[ERROR] Invalid option. Please enter a number between 1 and 19.");
            }

            if (running) {
                System.out.println("\nPress [ENTER] to return to the main menu...");
                scanner.nextLine();
            }
        }
    }

    private static void printWelcomeBanner() {
        System.out.println("==========================================================================");
        System.out.println("          ANNAM: SMART FOOD DISTRIBUTION & FOOD WASTE REDUCTION           ");
        System.out.println("             College Mini Project - Java OOP & DSA Simulation             ");
        System.out.println("==========================================================================");
        System.out.println("  Initialized with: 5 Donors | 4 Recipients | 3 Vehicles | 12 Locations   ");
        System.out.println("  Initial Simulated Time: 10:00 AM | Handover Allowance: 10 Minutes      ");
        System.out.println("==========================================================================");
    }

    private static void printMainMenu() {
        System.out.println();
        System.out.println("==========================================================================");
        System.out.println("                               MAIN MENU                                  ");
        System.out.println("==========================================================================");
        System.out.println("  1. View Current Simulated Time       11. Confirm Delivery");
        System.out.println("  2. Change Simulated Time             12. View Allocation History");
        System.out.println("  3. View Food Batches                 13. View Unallocated Remainders");
        System.out.println("  4. View Approved Food Priority Queue 14. Run Expiry Check");
        System.out.println("  5. Approve Food Batch                15. View Road Network");
        System.out.println("  6. View Recipients                   16. Find Shortest Route (Dijkstra)");
        System.out.println("  7. View Recipient Demand             17. View System Summary");
        System.out.println("  8. View Vehicles                     18. Run Test Scenarios (10 Tests)");
        System.out.println("  9. Create Food Allocation            19. Exit");
        System.out.println(" 10. Reserve Food");
        System.out.println("==========================================================================");
    }

    private static String prompt(String message) {
        System.out.print(message);
        return scanner.nextLine();
    }

    // ----------------------------------------------------
    // Option 1 & 2: Clock Management
    // ----------------------------------------------------

    private static void viewCurrentSimulatedTime() {
        System.out.println("\n-------------------------------------------");
        System.out.println("         CURRENT SIMULATED TIME            ");
        System.out.println("-------------------------------------------");
        System.out.println("Simulated Clock Time : " + service.getClock().getFormattedCurrentTime());
        System.out.println("Handover Allowance   : " + service.getClock().getHandoverMinutes() + " minutes");
        System.out.println("-------------------------------------------");
    }

    private static void changeSimulatedTime() {
        System.out.println("\n-------------------------------------------");
        System.out.println("          CHANGE SIMULATED TIME            ");
        System.out.println("-------------------------------------------");
        System.out.println("Current Time: " + service.getClock().getFormattedCurrentTime());
        String input = prompt("Enter new simulated time (HH:mm, e.g. 11:30): ");
        try {
            service.getClock().setTime(input.trim());
            System.out.println("[SUCCESS] Simulated time updated to: " + service.getClock().getFormattedCurrentTime());
            // Automatically prompt or trigger expiry notification
            List<FoodBatch> newlyExpired = service.runExpiryCheck();
            if (!newlyExpired.isEmpty()) {
                System.out.println("[NOTICE] " + newlyExpired.size() + " batch(es) reached or passed use-by time and were marked EXPIRED.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    // ----------------------------------------------------
    // Option 3 & 4: Food Batches & PriorityQueue
    // ----------------------------------------------------

    private static void viewFoodBatches() {
        System.out.println("\n==================================================================================================");
        System.out.println("                                      ALL FOOD BATCHES                                            ");
        System.out.println("==================================================================================================");
        System.out.printf("%-6s | %-16s | %-10s | %-8s | %-14s | %-16s | %-12s | %-8s%n",
                "BATCH", "CATEGORY", "PORTIONS", "USE-BY", "DONOR", "PICKUP LOC", "APPROVAL", "STATUS");
        System.out.println("--------------------------------------------------------------------------------------------------");
        for (FoodBatch b : service.getAllBatches()) {
            String portionStr = b.getAvailablePortions() + "/" + b.getTotalPortions();
            String status = b.isExpired() ? "EXPIRED" : (b.getAvailablePortions() == 0 ? "DEPLETED" : "AVAILABLE");
            System.out.printf("%-6s | %-16s | %-10s | %-8s | %-14s | %-16s | %-12s | %-8s%n",
                    b.getBatchId(),
                    b.getCategory().name(),
                    portionStr,
                    b.getDeclaredUseByTime().format(TIME_FMT),
                    b.getDonor().getDonorId(),
                    b.getPickupLocation().getName(),
                    b.getApprovalStatus(),
                    status);
        }
        System.out.println("==================================================================================================");
    }

    private static void viewApprovedFoodPriorityQueue() {
        List<FoodBatch> queueView = service.getApprovedBatchesInPriorityOrder();
        System.out.println("\n==================================================================================================");
        System.out.println("                 APPROVED FOOD BATCHES PRIORITY QUEUE (EARLIEST USE-BY FIRST)                     ");
        System.out.println("==================================================================================================");
        if (queueView.isEmpty()) {
            System.out.println("  [No approved active batches currently in queue]");
        } else {
            System.out.printf("%-5s | %-6s | %-16s | %-10s | %-8s | %-16s | %-16s%n",
                    "RANK", "BATCH", "CATEGORY", "AVAILABLE", "USE-BY", "DONOR", "PICKUP LOC");
            System.out.println("--------------------------------------------------------------------------------------------------");
            int rank = 1;
            for (FoodBatch b : queueView) {
                System.out.printf("#%-4d | %-6s | %-16s | %-10d | %-8s | %-16s | %-16s%n",
                        rank++,
                        b.getBatchId(),
                        b.getCategory().name(),
                        b.getAvailablePortions(),
                        b.getDeclaredUseByTime().format(TIME_FMT),
                        b.getDonor().getName(),
                        b.getPickupLocation().getName());
            }
        }
        System.out.println("==================================================================================================");
        System.out.println("Note: Order is deterministic based on Earliest Use-By Time, with Batch ID as tie-breaker.");
    }

    // ----------------------------------------------------
    // Option 5: Approve Batch
    // ----------------------------------------------------

    private static void approveFoodBatch() {
        System.out.println("\n-------------------------------------------");
        System.out.println("           APPROVE FOOD BATCH              ");
        System.out.println("-------------------------------------------");
        String batchId = prompt("Enter Batch ID to approve (e.g. B11): ");
        try {
            boolean success = service.approveBatch(batchId);
            if (success) {
                FoodBatch b = service.getBatch(batchId);
                System.out.println("[SUCCESS] Batch " + b.getBatchId() + " (" + b.getCategory() 
                        + ") approved and added to PriorityQueue.");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to approve batch: " + e.getMessage());
        }
    }

    // ----------------------------------------------------
    // Option 6 & 7: Recipients & Demand
    // ----------------------------------------------------

    private static void viewRecipients() {
        System.out.println("\n==================================================================================================");
        System.out.println("                                    REGISTERED RECIPIENTS                                         ");
        System.out.println("==================================================================================================");
        System.out.printf("%-6s | %-26s | %-16s | %-16s | %-12s%n",
                "ID", "NAME", "TYPE", "LOCATION", "DEMAND");
        System.out.println("--------------------------------------------------------------------------------------------------");
        for (Recipient r : service.getAllRecipients()) {
            String demandStr = r.getRemainingDemandPortions() + "/" + r.getTotalRequiredPortions() + " portions";
            System.out.printf("%-6s | %-26s | %-16s | %-16s | %-12s%n",
                    r.getRecipientId(),
                    r.getName(),
                    r.getRecipientType(),
                    r.getLocation().getName(),
                    demandStr);
        }
        System.out.println("==================================================================================================");
    }

    private static void viewRecipientDemand() {
        System.out.println("\n==================================================================================================");
        System.out.println("                           RECIPIENT DEMAND & ACCEPTED FOOD CATEGORIES                            ");
        System.out.println("==================================================================================================");
        for (Recipient r : service.getAllRecipients()) {
            System.out.printf("Recipient [%s] : %s (%s)%n", r.getRecipientId(), r.getName(), r.getRecipientType());
            System.out.printf("  Location        : %s%n", r.getLocation().getName());
            System.out.printf("  Demand Progress : %d / %d portions remaining%n",
                    r.getRemainingDemandPortions(), r.getTotalRequiredPortions());
            System.out.print("  Accepted Foods  : ");
            StringBuilder sb = new StringBuilder();
            for (FoodCategory c : r.getAcceptedCategories()) {
                sb.append(c.name()).append(", ");
            }
            if (sb.length() > 2) sb.setLength(sb.length() - 2);
            System.out.println(sb.toString());
            System.out.println("--------------------------------------------------------------------------------------------------");
        }
    }

    // ----------------------------------------------------
    // Option 8: Vehicles
    // ----------------------------------------------------

    private static void viewVehicles() {
        System.out.println("\n==================================================================================================");
        System.out.println("                                    DELIVERY VEHICLES                                             ");
        System.out.println("==================================================================================================");
        System.out.printf("%-6s | %-20s | %-18s | %-14s | %-12s%n",
                "ID", "VEHICLE NAME", "CURRENT LOCATION", "CAPACITY", "STATUS");
        System.out.println("--------------------------------------------------------------------------------------------------");
        for (DeliveryVehicle v : service.getAllVehicles()) {
            System.out.printf("%-6s | %-20s | %-18s | %-14s | %-12s%n",
                    v.getVehicleId(),
                    v.getName(),
                    v.getCurrentLocation().getName(),
                    v.getPortionCapacity() + " portions",
                    (v.isAvailable() ? "AVAILABLE" : "BUSY"));
        }
        System.out.println("==================================================================================================");
    }

    // ----------------------------------------------------
    // Option 9, 10, 11: Matching, Allocation & Reservation
    // ----------------------------------------------------

    private static void createFoodAllocation() {
        System.out.println("\n-------------------------------------------");
        System.out.println("          CREATE FOOD ALLOCATION           ");
        System.out.println("-------------------------------------------");
        String batchId = prompt("Enter Batch ID (e.g. B01): ");
        String recipientId = prompt("Enter Recipient ID (e.g. R01): ");
        String vehicleId = prompt("Enter Vehicle ID (e.g. V01): ");

        FoodBatch batch = service.getBatch(batchId);
        Recipient recipient = service.getRecipient(recipientId);
        DeliveryVehicle vehicle = service.getVehicle(vehicleId);

        if (batch == null || recipient == null || vehicle == null) {
            System.out.println("[ERROR] Invalid IDs provided. Batch, recipient, and vehicle must all exist.");
            return;
        }

        // Evaluate match
        MatchResult result = service.simulateMatch(batchId, recipientId, vehicleId);

        System.out.println("\n-------------------------------------------");
        System.out.println("FOOD MATCH RESULT");
        System.out.println("-------------------------------------------");
        System.out.printf("Batch ID        : %s%n", batch.getBatchId());
        System.out.printf("Food Category   : %s%n", batch.getCategory().name());
        System.out.printf("Available       : %d portions%n", batch.getAvailablePortions());
        System.out.printf("Recipient       : %s (%s)%n", recipient.getRecipientId(), recipient.getName());
        System.out.printf("Demand          : %d portions%n", recipient.getRemainingDemandPortions());
        System.out.printf("Vehicle         : %s (%s)%n", vehicle.getVehicleId(), vehicle.getName());
        System.out.printf("Vehicle Capacity: %d portions%n", vehicle.getPortionCapacity());
        System.out.println();

        if (result.isAccepted()) {
            System.out.println("Route:");
            System.out.println(result.getRouteResult().getFormattedRoute());
            System.out.println();
            System.out.printf("Travel Time    : %d minutes%n", result.getTravelTimeMinutes());
            System.out.printf("Pickup Time    : %s%n", result.getPickupTime().format(TIME_FMT));
            System.out.printf("Handover       : %d minutes%n", result.getHandoverMinutes());
            System.out.printf("Completion     : %s%n", result.getExpectedCompletionTime().format(TIME_FMT));
            System.out.printf("Use-By         : %s%n", result.getUseByTime().format(TIME_FMT));
            System.out.println();
            System.out.println("STATUS: MATCH ACCEPTED");
            System.out.printf("Allocated      : %d portions%n", result.getAllocatedPortions());
            int remainingFood = batch.getAvailablePortions() - result.getAllocatedPortions();
            System.out.printf("Remaining Food : %d portions%n", remainingFood);
            System.out.println("-------------------------------------------");

            String proceed = prompt("Would you like to instantiate this PROPOSED allocation? (y/n): ");
            if (proceed.trim().equalsIgnoreCase("y")) {
                try {
                    FoodAllocation alloc = service.createAllocation(batchId, recipientId, vehicleId);
                    System.out.println("[SUCCESS] Created Allocation: " + alloc.getAllocationId() 
                            + " (Status: " + alloc.getStatus() + ")");
                    System.out.println("Tip: Use Option 10 to officially RESERVE these portions.");
                } catch (Exception e) {
                    System.out.println("[ERROR] Failed to create allocation: " + e.getMessage());
                }
            }
        } else {
            System.out.println("STATUS: MATCH REJECTED");
            System.out.println("Reason:");
            System.out.println(result.getReason());
            System.out.println("-------------------------------------------");
        }
    }

    private static void reserveFood() {
        System.out.println("\n-------------------------------------------");
        System.out.println("               RESERVE FOOD                ");
        System.out.println("-------------------------------------------");
        String allocId = prompt("Enter Allocation ID to reserve (e.g. ALC-001): ");
        FoodAllocation alloc = service.getAllocation(allocId);

        if (alloc == null) {
            System.out.println("[ERROR] Allocation ID " + allocId + " not found.");
            return;
        }

        try {
            service.reserveAllocation(alloc);
            System.out.println("[SUCCESS] Allocation " + alloc.getAllocationId() + " successfully RESERVED!");
            System.out.println("Portions Reserved : " + alloc.getAllocatedPortions());
            System.out.println("Vehicle Assigned  : " + alloc.getVehicle().getName() + " (Marked BUSY)");
            System.out.println("Batch Balance     : " + alloc.getFoodBatch().getAvailablePortions() + " portions available");
            System.out.println("Recipient Demand  : " + alloc.getRecipient().getRemainingDemandPortions() + " portions remaining");
        } catch (AnnamException e) {
            System.out.println("[ERROR] Reservation rejected: " + e.getMessage());
        }
    }

    private static void confirmDelivery() {
        System.out.println("\n-------------------------------------------");
        System.out.println("             CONFIRM DELIVERY              ");
        System.out.println("-------------------------------------------");
        String allocId = prompt("Enter Allocation ID to confirm delivery (e.g. ALC-001): ");
        try {
            service.confirmDelivery(allocId);
            FoodAllocation alloc = service.getAllocation(allocId);
            System.out.println("[SUCCESS] Delivery confirmed for Allocation " + alloc.getAllocationId() + "!");
            System.out.println("Food safely handed over to " + alloc.getRecipient().getName() + ".");
            System.out.println("Vehicle " + alloc.getVehicle().getName() + " reached " 
                    + alloc.getDestinationLocation().getName() + " and is now AVAILABLE for next trip.");
        } catch (AnnamException e) {
            System.out.println("[ERROR] Delivery confirmation rejected: " + e.getMessage());
        }
    }

    // ----------------------------------------------------
    // Option 12 & 13: History & Remainders
    // ----------------------------------------------------

    private static void viewAllocationHistory() {
        System.out.println("\n==================================================================================================");
        System.out.println("                                    ALLOCATION HISTORY                                            ");
        System.out.println("==================================================================================================");
        List<FoodAllocation> history = service.getAllocations();
        if (history.isEmpty()) {
            System.out.println("  [No allocations created yet]");
        } else {
            System.out.printf("%-8s | %-6s | %-24s | %-8s | %-8s | %-8s | %-12s%n",
                    "ALLOC ID", "BATCH", "RECIPIENT", "QTY", "TRAVEL", "COMPLETION", "STATUS");
            System.out.println("--------------------------------------------------------------------------------------------------");
            for (FoodAllocation a : history) {
                System.out.printf("%-8s | %-6s | %-24s | %-8d | %-8s | %-10s | %-12s%n",
                        a.getAllocationId(),
                        a.getFoodBatch().getBatchId(),
                        a.getRecipient().getName(),
                        a.getAllocatedPortions(),
                        a.getTravelTimeMinutes() + "m",
                        a.getExpectedCompletionTime().format(TIME_FMT),
                        a.getStatus());
            }
        }
        System.out.println("==================================================================================================");
    }

    private static void viewUnallocatedRemainders() {
        System.out.println("\n==================================================================================================");
        System.out.println("                                UNALLOCATED FOOD REMAINDERS                                       ");
        System.out.println("==================================================================================================");
        List<FoodBatch> remainders = service.getUnallocatedRemainders();
        if (remainders.isEmpty()) {
            System.out.println("  [No unallocated remainders currently recorded]");
        } else {
            System.out.printf("%-6s | %-16s | %-10s | %-10s | %-8s | %-16s%n",
                    "BATCH", "CATEGORY", "AVAILABLE", "RESERVED", "USE-BY", "PICKUP LOC");
            System.out.println("--------------------------------------------------------------------------------------------------");
            for (FoodBatch b : remainders) {
                System.out.printf("%-6s | %-16s | %-10d | %-10d | %-8s | %-16s%n",
                        b.getBatchId(),
                        b.getCategory().name(),
                        b.getAvailablePortions(),
                        b.getReservedPortions(),
                        b.getDeclaredUseByTime().format(TIME_FMT),
                        b.getPickupLocation().getName());
            }
        }
        System.out.println("==================================================================================================");
        System.out.println("Notice: Remainder quantities are retained and remain available for subsequent distributions.");
    }

    // ----------------------------------------------------
    // Option 14: Expiry Check
    // ----------------------------------------------------

    private static void runExpiryCheck() {
        System.out.println("\n-------------------------------------------");
        System.out.println("             RUN EXPIRY CHECK              ");
        System.out.println("-------------------------------------------");
        System.out.println("Current Simulated Time: " + service.getClock().getFormattedCurrentTime());
        List<FoodBatch> expiredList = service.runExpiryCheck();
        if (expiredList.isEmpty()) {
            System.out.println("[INFO] No batches have expired at current simulated time.");
        } else {
            System.out.println("[WARNING] The following " + expiredList.size() + " batch(es) have expired and were removed from PriorityQueue:");
            for (FoodBatch b : expiredList) {
                System.out.printf("  - %s (%s) | Use-By: %s | Portions Forfeited: %d%n",
                        b.getBatchId(), b.getCategory().name(),
                        b.getDeclaredUseByTime().format(TIME_FMT), b.getAvailablePortions());
            }
        }
    }

    // ----------------------------------------------------
    // Option 15 & 16: Road Network & Dijkstra
    // ----------------------------------------------------

    private static void viewRoadNetwork() {
        service.getRoadNetwork().printNetworkSummary();
    }

    private static void findShortestRoute() {
        System.out.println("\n-------------------------------------------");
        System.out.println("     FIND SHORTEST ROUTE (DIJKSTRA)        ");
        System.out.println("-------------------------------------------");
        String srcId = prompt("Enter Source Location ID (e.g. LOC01): ").trim().toUpperCase();
        String destId = prompt("Enter Destination Location ID (e.g. LOC07): ").trim().toUpperCase();

        Location src = service.getRoadNetwork().getLocation(srcId);
        Location dest = service.getRoadNetwork().getLocation(destId);

        if (src == null || dest == null) {
            System.out.println("[ERROR] Invalid location ID. Please check Option 15 for valid locations.");
            return;
        }

        PathResult result = DijkstraAlgorithm.findShortestPath(service.getRoadNetwork(), src, dest);
        System.out.println("\n-------------------------------------------");
        System.out.println("DIJKSTRA SHORTEST PATH RESULT");
        System.out.println("-------------------------------------------");
        System.out.println("From        : " + src.getName() + " (" + src.getLocationId() + ")");
        System.out.println("To          : " + dest.getName() + " (" + dest.getLocationId() + ")");
        System.out.println("Reachable   : " + (result.isReachable() ? "YES" : "NO"));
        if (result.isReachable()) {
            System.out.println("Route       : " + result.getFormattedRoute());
            System.out.println("Travel Time : " + result.getTotalTravelTimeMinutes() + " minutes");
        }
        System.out.println("-------------------------------------------");
    }

    // ----------------------------------------------------
    // Option 17: System Summary
    // ----------------------------------------------------

    private static void viewSystemSummary() {
        System.out.println("\n==========================================================================");
        System.out.println("                   ANNAM NETWORK SYSTEM SUMMARY                           ");
        System.out.println("==========================================================================");
        System.out.printf("Current Simulated Clock    : %s (Handover: %d mins)%n",
                service.getClock().getFormattedCurrentTime(), service.getClock().getHandoverMinutes());
        System.out.printf("Total Donors Registered    : %d%n", service.getAllDonors().size());
        System.out.printf("Total Food Batches         : %d%n", service.getAllBatches().size());

        long approvedCount = service.getAllBatches().stream().filter(FoodBatch::isApproved).count();
        long expiredCount = service.getAllBatches().stream().filter(FoodBatch::isExpired).count();
        int queueCount = service.getApprovedBatchesInPriorityOrder().size();

        System.out.printf("  - Approved Batches       : %d%n", approvedCount);
        System.out.printf("  - Expired Batches        : %d%n", expiredCount);
        System.out.printf("  - Active in PriorityQueue: %d%n", queueCount);

        int totalRemainingDemand = service.getAllRecipients().stream()
                .mapToInt(Recipient::getRemainingDemandPortions).sum();
        System.out.printf("Total Recipients           : %d (Unfulfilled Demand: %d portions)%n",
                service.getAllRecipients().size(), totalRemainingDemand);

        long availableVehicles = service.getAllVehicles().stream()
                .filter(DeliveryVehicle::isAvailable).count();
        System.out.printf("Delivery Vehicles          : %d total (%d available)%n",
                service.getAllVehicles().size(), availableVehicles);

        long deliveredAllocations = service.getAllocations().stream()
                .filter(a -> a.getStatus() == AllocationStatus.DELIVERED).count();
        int totalPortionsDelivered = service.getAllocations().stream()
                .filter(a -> a.getStatus() == AllocationStatus.DELIVERED)
                .mapToInt(FoodAllocation::getAllocatedPortions).sum();

        System.out.printf("Allocations Completed      : %d (Total Portions Delivered: %d)%n",
                deliveredAllocations, totalPortionsDelivered);
        System.out.printf("Unallocated Remainders     : %d active batches%n",
                service.getUnallocatedRemainders().size());
        System.out.println("==========================================================================");
    }

    // ----------------------------------------------------
    // Option 18: Run Automated Test Scenarios
    // ----------------------------------------------------

    private static void runTestScenarios() {
        SystemTestRunner runner = new SystemTestRunner();
        runner.runAllTests();
    }
}
