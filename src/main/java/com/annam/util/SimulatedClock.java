package com.annam.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Manages the simulated timeline for the Annam network without thread sleeping or altering OS time.
 */
public class SimulatedClock {
    public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    public static final int DEFAULT_HANDOVER_MINUTES = 10;

    private LocalTime currentTime;
    private int handoverAllowanceMinutes;

    public SimulatedClock() {
        this(LocalTime.of(10, 0), DEFAULT_HANDOVER_MINUTES);
    }

    public SimulatedClock(LocalTime initialTime, int handoverAllowanceMinutes) {
        this.currentTime = initialTime != null ? initialTime : LocalTime.of(10, 0);
        this.handoverAllowanceMinutes = handoverAllowanceMinutes > 0 ? handoverAllowanceMinutes : DEFAULT_HANDOVER_MINUTES;
    }

    public synchronized LocalTime getCurrentTime() {
        return currentTime;
    }

    public synchronized void setCurrentTime(LocalTime newTime) {
        if (newTime == null) {
            throw new IllegalArgumentException("Simulated time cannot be null.");
        }
        this.currentTime = newTime;
    }

    public synchronized void advanceMinutes(int minutes) {
        if (minutes <= 0) return;
        this.currentTime = this.currentTime.plusMinutes(minutes);
    }

    public synchronized int getHandoverAllowanceMinutes() {
        return handoverAllowanceMinutes;
    }

    public synchronized void setHandoverAllowanceMinutes(int handoverAllowanceMinutes) {
        if (handoverAllowanceMinutes < 0) {
            throw new IllegalArgumentException("Handover allowance cannot be negative.");
        }
        this.handoverAllowanceMinutes = handoverAllowanceMinutes;
    }

    public synchronized String getFormattedTime() {
        return currentTime.format(TIME_FMT);
    }

    @Override
    public synchronized String toString() {
        return "SimulatedClock [" + getFormattedTime() + "] (Handover: " + handoverAllowanceMinutes + " mins)";
    }
}
