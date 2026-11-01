package com.annam.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deterministic unique ID generator for entities.
 */
public class IDGenerator {
    private static final AtomicInteger ALLOCATION_COUNTER = new AtomicInteger(100);

    public static String nextAllocationId() {
        return String.format("ALC%04d", ALLOCATION_COUNTER.incrementAndGet());
    }

    public static void setCounter(int val) {
        ALLOCATION_COUNTER.set(val);
    }
}
