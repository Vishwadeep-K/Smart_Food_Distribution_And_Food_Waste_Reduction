package util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Manages the simulated timeline of the Annam distribution network.
 * Avoids real-time waiting or Thread.sleep(), enabling deterministic simulation and testing.
 */
public class SimulatedClock {
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    public static final int DEFAULT_HANDOVER_MINUTES = 10;

    private LocalTime currentTime;
    private final int handoverMinutes;

    public SimulatedClock() {
        this(LocalTime.of(10, 0), DEFAULT_HANDOVER_MINUTES); // Default start time 10:00 AM
    }

    public SimulatedClock(LocalTime initialTime, int handoverMinutes) {
        if (initialTime == null) {
            throw new IllegalArgumentException("Initial simulated time cannot be null.");
        }
        if (handoverMinutes < 0) {
            throw new IllegalArgumentException("Handover allowance cannot be negative.");
        }
        this.currentTime = initialTime;
        this.handoverMinutes = handoverMinutes;
    }

    public synchronized LocalTime getCurrentTime() {
        return currentTime;
    }

    public synchronized String getFormattedCurrentTime() {
        return currentTime.format(TIME_FORMATTER);
    }

    public synchronized void setTime(LocalTime newTime) {
        if (newTime == null) {
            throw new IllegalArgumentException("Simulated time cannot be null.");
        }
        this.currentTime = newTime;
    }

    public synchronized void setTime(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            throw new IllegalArgumentException("Time string cannot be empty.");
        }
        try {
            this.currentTime = LocalTime.parse(timeString.trim(), TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time format. Please use HH:mm (e.g. 10:30, 14:15).");
        }
    }

    public synchronized void advanceMinutes(int minutes) {
        if (minutes < 0) {
            throw new IllegalArgumentException("Cannot advance clock backwards.");
        }
        this.currentTime = this.currentTime.plusMinutes(minutes);
    }

    public int getHandoverMinutes() {
        return handoverMinutes;
    }

    @Override
    public synchronized String toString() {
        return "Simulated Clock: " + getFormattedCurrentTime() + " (Handover Allowance: " + handoverMinutes + " mins)";
    }
}
