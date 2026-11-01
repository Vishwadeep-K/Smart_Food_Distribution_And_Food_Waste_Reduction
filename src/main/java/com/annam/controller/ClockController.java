package com.annam.controller;

import com.annam.service.ExpiryService;
import com.annam.service.SimulatedClockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clock")
public class ClockController {
    private final SimulatedClockService clockService;
    private final ExpiryService expiryService;

    public ClockController(SimulatedClockService clockService, ExpiryService expiryService) {
        this.clockService = clockService;
        this.expiryService = expiryService;
    }

    @GetMapping
    public ResponseEntity<?> getClock() {
        return ResponseEntity.ok(Map.of(
                "simulatedTime", clockService.getFormattedTime(),
                "handoverAllowanceMinutes", clockService.getHandoverAllowanceMinutes()
        ));
    }

    @PostMapping("/set")
    public ResponseEntity<?> setClock(@RequestBody Map<String, Object> body) {
        String timeStr = (String) body.get("time");
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "time string is required (HH:mm)."));
        }

        try {
            if (timeStr.length() == 5) timeStr += ":00";
            LocalTime time = LocalTime.parse(timeStr);
            clockService.setTime(time);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid time format (HH:mm)."));
        }

        if (body.get("handover") instanceof Number handoverNum) {
            clockService.setHandoverAllowanceMinutes(handoverNum.intValue());
        }

        // Automatically trigger expiry check upon setting new time
        List<String> expired = expiryService.runExpiryCheck();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Simulated time updated to " + clockService.getFormattedTime() + ".",
                "simulatedTime", clockService.getFormattedTime(),
                "handoverAllowanceMinutes", clockService.getHandoverAllowanceMinutes(),
                "expiredBatches", expired
        ));
    }

    @PostMapping("/advance")
    public ResponseEntity<?> advanceClock(@RequestBody Map<String, Integer> body) {
        Integer minutes = body.get("minutes");
        if (minutes == null || minutes <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Positive minutes value required."));
        }

        clockService.advanceMinutes(minutes);
        List<String> expired = expiryService.runExpiryCheck();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Advanced clock by " + minutes + " minutes.",
                "simulatedTime", clockService.getFormattedTime(),
                "expiredBatches", expired
        ));
    }

    @PostMapping("/run-expiry")
    public ResponseEntity<?> runExpiryCheck() {
        List<String> expired = expiryService.runExpiryCheck();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "currentTime", clockService.getFormattedTime(),
                "expiredCount", expired.size(),
                "expiredBatchIds", expired
        ));
    }
}
