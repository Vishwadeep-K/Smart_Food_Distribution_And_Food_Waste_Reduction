package service;

import exception.FoodExpiredException;
import exception.InvalidAllocationException;
import graph.PathResult;
import graph.RoadNetwork;
import java.time.LocalTime;
import java.util.*;
import model.*;
import policy.FoodMatchingPolicy;
import policy.MatchResult;
import policy.TimeSensitiveFoodMatchingPolicy;
import util.SimulatedClock;

/**
 * Central coordinator service for the Annam food distribution network.
 * Manages the primary data structures:
 * - PriorityQueue for earliest-use-by-first approved food batches (with deterministic tie-breaker)
 * - HashMaps for fast O(1) lookups of Batches, Recipients, Vehicles, Donors
 * - Custom Adjacency-List RoadNetwork
 * - ArrayLists for allocation history and unallocated remainders
 */
public class FoodDistributionService {
    private final Map<String, FoodBatch> batchMap;
    private final Map<String, Recipient> recipientMap;
    private final Map<String, DeliveryVehicle> vehicleMap;
    private final Map<String, Donor> donorMap;

    private final PriorityQueue<FoodBatch> approvedFoodQueue;
    private final RoadNetwork roadNetwork;
    private final SimulatedClock clock;
    private final FoodMatchingPolicy matchingPolicy;
    private final AllocationService allocationService;
    private final ExpiryService expiryService;

    public FoodDistributionService(RoadNetwork roadNetwork, SimulatedClock clock) {
        this.batchMap = new LinkedHashMap<>();
        this.recipientMap = new LinkedHashMap<>();
        this.vehicleMap = new LinkedHashMap<>();
        this.donorMap = new LinkedHashMap<>();

        // PriorityQueue orders by earliest use-by time first, with Batch ID tie-breaker
        this.approvedFoodQueue = new PriorityQueue<>();
        this.roadNetwork = roadNetwork != null ? roadNetwork : new RoadNetwork();
        this.clock = clock != null ? clock : new SimulatedClock();
        this.matchingPolicy = new TimeSensitiveFoodMatchingPolicy();
        this.allocationService = new AllocationService(this.matchingPolicy);
        this.expiryService = new ExpiryService();
    }

    // ----------------------------------------------------
    // Registration & Ingestion Methods
    // ----------------------------------------------------

    public void registerDonor(Donor donor) {
        if (donor == null) return;
        donorMap.put(donor.getDonorId().toUpperCase(), donor);
    }

    public void registerRecipient(Recipient recipient) {
        if (recipient == null) return;
        recipientMap.put(recipient.getRecipientId().toUpperCase(), recipient);
    }

    public void registerVehicle(DeliveryVehicle vehicle) {
        if (vehicle == null) return;
        vehicleMap.put(vehicle.getVehicleId().toUpperCase(), vehicle);
    }

    /**
     * Registers a FoodBatch in the central registry.
     * If already marked APPROVED and not expired, places it into the PriorityQueue.
     */
    public void registerBatch(FoodBatch batch) {
        if (batch == null) return;
        String id = batch.getBatchId().toUpperCase();
        batchMap.put(id, batch);

        if (batch.isApproved() && !batch.isExpired() && batch.getAvailablePortions() > 0) {
            // Ensure no duplicate entry in queue
            approvedFoodQueue.remove(batch);
            approvedFoodQueue.add(batch);
        }
    }

    // ----------------------------------------------------
    // Batch Approval & PriorityQueue Synchronization
    // ----------------------------------------------------

    /**
     * Approves a food batch by ID.
     * Strictly follows PriorityQueue rules:
     * - Remove from queue if present
     * - Update approval status
     * - Re-insert into PriorityQueue
     */
    public synchronized boolean approveBatch(String batchId) {
        FoodBatch batch = getBatch(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("Batch ID " + batchId + " not found.");
        }
        if (batch.isExpired()) {
            throw new FoodExpiredException("Cannot approve expired batch " + batchId + ".");
        }

        // Remove from queue before state change if present
        approvedFoodQueue.remove(batch);

        // Update state
        batch.setApprovalStatus(ApprovalStatus.APPROVED);

        // Reinsert only if it has available portions and not expired
        if (!batch.isExpired() && batch.getAvailablePortions() > 0) {
            approvedFoodQueue.add(batch);
        }
        return true;
    }

    /**
     * Updates portions or status of a batch while maintaining PriorityQueue invariants.
     */
    public synchronized void syncBatchInQueue(FoodBatch batch) {
        if (batch == null) return;
        approvedFoodQueue.remove(batch);
        if (batch.isApproved() && !batch.isExpired() && batch.getAvailablePortions() > 0) {
            approvedFoodQueue.add(batch);
        }
    }

    // ----------------------------------------------------
    // Expiry Processing
    // ----------------------------------------------------

    /**
     * Scans all batches against current simulated clock time.
     * Expired batches are marked EXPIRED and purged from the approved PriorityQueue.
     */
    public synchronized List<FoodBatch> runExpiryCheck() {
        LocalTime now = clock.getCurrentTime();
        List<FoodBatch> expired = expiryService.scanAndMarkExpired(batchMap.values(), now);

        for (FoodBatch b : expired) {
            approvedFoodQueue.remove(b);
        }
        return expired;
    }

    // ----------------------------------------------------
    // Matching & Allocation
    // ----------------------------------------------------

    public MatchResult simulateMatch(String batchId, String recipientId, String vehicleId) {
        FoodBatch batch = getBatch(batchId);
        Recipient recipient = getRecipient(recipientId);
        DeliveryVehicle vehicle = getVehicle(vehicleId);

        return matchingPolicy.evaluateMatch(
                batch, recipient, vehicle, roadNetwork, clock.getCurrentTime(), clock.getHandoverMinutes()
        );
    }

    public FoodAllocation createAllocation(String batchId, String recipientId, String vehicleId) {
        FoodBatch batch = getBatch(batchId);
        Recipient recipient = getRecipient(recipientId);
        DeliveryVehicle vehicle = getVehicle(vehicleId);

        if (batch == null) throw new InvalidAllocationException("Batch not found: " + batchId);
        if (recipient == null) throw new InvalidAllocationException("Recipient not found: " + recipientId);
        if (vehicle == null) throw new InvalidAllocationException("Vehicle not found: " + vehicleId);

        return allocationService.createProposedAllocation(
                batch, recipient, vehicle, roadNetwork, clock.getCurrentTime(), clock.getHandoverMinutes()
        );
    }

    public synchronized void reserveAllocation(FoodAllocation allocation) {
        allocationService.reserveAllocation(allocation);
        // Sync batch in priority queue
        syncBatchInQueue(allocation.getFoodBatch());
    }

    public synchronized void confirmDelivery(String allocationId) {
        FoodAllocation allocation = getAllocation(allocationId);
        if (allocation == null) {
            throw new InvalidAllocationException("Allocation ID " + allocationId + " not found.");
        }
        allocationService.confirmDelivery(allocation);
    }

    // ----------------------------------------------------
    // Lookup & Retrieval Methods (O(1) HashMaps)
    // ----------------------------------------------------

    public FoodBatch getBatch(String batchId) {
        if (batchId == null) return null;
        return batchMap.get(batchId.trim().toUpperCase());
    }

    public Recipient getRecipient(String recipientId) {
        if (recipientId == null) return null;
        return recipientMap.get(recipientId.trim().toUpperCase());
    }

    public DeliveryVehicle getVehicle(String vehicleId) {
        if (vehicleId == null) return null;
        return vehicleMap.get(vehicleId.trim().toUpperCase());
    }

    public Donor getDonor(String donorId) {
        if (donorId == null) return null;
        return donorMap.get(donorId.trim().toUpperCase());
    }

    public FoodAllocation getAllocation(String allocationId) {
        if (allocationId == null) return null;
        for (FoodAllocation a : allocationService.getAllocationHistory()) {
            if (a.getAllocationId().equalsIgnoreCase(allocationId.trim())) {
                return a;
            }
        }
        return null;
    }

    public Collection<FoodBatch> getAllBatches() {
        return Collections.unmodifiableCollection(batchMap.values());
    }

    public Collection<Recipient> getAllRecipients() {
        return Collections.unmodifiableCollection(recipientMap.values());
    }

    public Collection<DeliveryVehicle> getAllVehicles() {
        return Collections.unmodifiableCollection(vehicleMap.values());
    }

    public Collection<Donor> getAllDonors() {
        return Collections.unmodifiableCollection(donorMap.values());
    }

    public List<FoodAllocation> getAllocations() {
        return allocationService.getAllocationHistory();
    }

    public List<FoodBatch> getUnallocatedRemainders() {
        return allocationService.getUnallocatedRemainders();
    }

    /**
     * Returns a sorted list view of the current approved batches in the PriorityQueue.
     * Orders earliest use-by time first, with Batch ID tie-breaker.
     */
    public List<FoodBatch> getApprovedBatchesInPriorityOrder() {
        PriorityQueue<FoodBatch> copy = new PriorityQueue<>(approvedFoodQueue);
        List<FoodBatch> list = new ArrayList<>();
        while (!copy.isEmpty()) {
            list.add(copy.poll());
        }
        return list;
    }

    public RoadNetwork getRoadNetwork() {
        return roadNetwork;
    }

    public SimulatedClock getClock() {
        return clock;
    }
}
