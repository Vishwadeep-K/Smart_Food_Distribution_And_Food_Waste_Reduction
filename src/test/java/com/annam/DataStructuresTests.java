package com.annam;

import com.annam.graph.Location;
import com.annam.model.ApprovalStatus;
import com.annam.model.Donor;
import com.annam.model.FoodBatch;
import com.annam.model.FoodCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.*;

public class DataStructuresTests {

    @Test
    @DisplayName("TEST 9: PriorityQueue Ordering (Earliest Use-By First)")
    void test09_PriorityQueueOrdering() {
        PriorityQueue<FoodBatch> queue = new PriorityQueue<>();
        Location loc = new Location("LOC01", "Hub", "Transit");
        Donor donor = new Donor("D01", "Hotel", "Org", "123", loc);

        FoodBatch b1 = new FoodBatch("B01", FoodCategory.MEALS, 20, LocalTime.of(14, 0), donor, loc, ApprovalStatus.APPROVED);
        FoodBatch b2 = new FoodBatch("B02", FoodCategory.RICE, 20, LocalTime.of(11, 30), donor, loc, ApprovalStatus.APPROVED);
        FoodBatch b3 = new FoodBatch("B03", FoodCategory.BREAD, 20, LocalTime.of(13, 15), donor, loc, ApprovalStatus.APPROVED);

        queue.add(b1);
        queue.add(b2);
        queue.add(b3);

        // B02 (11:30) must be polled first, followed by B03 (13:15), then B01 (14:00)
        assertEquals("B02", queue.poll().getBatchId());
        assertEquals("B03", queue.poll().getBatchId());
        assertEquals("B01", queue.poll().getBatchId());
    }

    @Test
    @DisplayName("TEST 10: Equal Expiry-Time Deterministic Tie-Breaking (by Batch ID)")
    void test10_EqualExpiryTieBreaker() {
        PriorityQueue<FoodBatch> queue = new PriorityQueue<>();
        Location loc = new Location("LOC01", "Hub", "Transit");
        Donor donor = new Donor("D01", "Hotel", "Org", "123", loc);

        // Both batches have the EXACT same use-by time: 13:00
        FoodBatch bZ = new FoodBatch("BZ", FoodCategory.MEALS, 20, LocalTime.of(13, 0), donor, loc, ApprovalStatus.APPROVED);
        FoodBatch bA = new FoodBatch("BA", FoodCategory.RICE, 20, LocalTime.of(13, 0), donor, loc, ApprovalStatus.APPROVED);

        queue.add(bZ);
        queue.add(bA);

        // Deterministic tie-breaker: BA must precede BZ lexicographically
        assertEquals("BA", queue.poll().getBatchId(), "Tie-breaker must deterministically prioritize BA over BZ");
        assertEquals("BZ", queue.poll().getBatchId());
    }

    @Test
    @DisplayName("PriorityQueue removal and re-insertion synchronization rule")
    void testPriorityQueueReinsertionOnUpdate() {
        PriorityQueue<FoodBatch> queue = new PriorityQueue<>();
        Location loc = new Location("LOC01", "Hub", "Transit");
        Donor donor = new Donor("D01", "Hotel", "Org", "123", loc);

        FoodBatch b1 = new FoodBatch("B01", FoodCategory.MEALS, 20, LocalTime.of(14, 0), donor, loc, ApprovalStatus.APPROVED);
        queue.add(b1);

        // Rule: Remove before altering ranking/state
        queue.remove(b1);
        b1.reservePortions(10);
        queue.add(b1);

        assertEquals(1, queue.size());
        assertEquals(10, queue.peek().getAvailablePortions());
    }

    @Test
    @DisplayName("HashMap O(1) Fast Lookups")
    void testHashMapLookups() {
        Map<String, FoodBatch> map = new HashMap<>();
        Location loc = new Location("LOC01", "Hub", "Transit");
        Donor donor = new Donor("D01", "Hotel", "Org", "123", loc);

        FoodBatch b = new FoodBatch("B100", FoodCategory.MEALS, 50, LocalTime.of(12, 0), donor, loc, ApprovalStatus.APPROVED);
        map.put("B100", b);

        assertTrue(map.containsKey("B100"));
        assertEquals(50, map.get("B100").getTotalPortions());
    }
}
