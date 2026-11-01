package com.annam.service;

import com.annam.repository.SettingsRepository;
import com.annam.util.SimulatedClock;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
public class SimulatedClockService {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private final SettingsRepository settingsRepository;
    private final SimulatedClock clock;

    public SimulatedClockService(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
        LocalTime savedTime = settingsRepository.getSimulatedTime();
        int savedHandover = settingsRepository.getHandoverMinutes();
        this.clock = new SimulatedClock(savedTime, savedHandover);
    }

    public synchronized LocalTime getCurrentTime() {
        return clock.getCurrentTime();
    }

    public synchronized String getFormattedTime() {
        return clock.getFormattedTime();
    }

    public synchronized void setTime(LocalTime time) {
        if (time == null) return;
        clock.setCurrentTime(time);
        settingsRepository.setSimulatedTime(time);
    }

    public synchronized void advanceMinutes(int minutes) {
        clock.advanceMinutes(minutes);
        settingsRepository.setSimulatedTime(clock.getCurrentTime());
    }

    public synchronized int getHandoverAllowanceMinutes() {
        return clock.getHandoverAllowanceMinutes();
    }

    public synchronized void setHandoverAllowanceMinutes(int minutes) {
        clock.setHandoverAllowanceMinutes(minutes);
        settingsRepository.setHandoverMinutes(minutes);
    }
}
