package util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deterministic generator for system identifiers (e.g. ALC-001, ALC-002).
 */
public class IDGenerator {
    private static final AtomicInteger ALLOCATION_COUNTER = new AtomicInteger(1);

    public static String nextAllocationId() {
        return String.format("ALC-%03d", ALLOCATION_COUNTER.getAndIncrement());
    }

    public static void reset() {
        ALLOCATION_COUNTER.set(1);
    }
}
