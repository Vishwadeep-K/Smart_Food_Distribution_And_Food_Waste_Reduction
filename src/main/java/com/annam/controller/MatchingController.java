package com.annam.controller;

import com.annam.policy.MatchResult;
import com.annam.service.MatchingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/matching")
public class MatchingController {
    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @GetMapping("/candidates")
    public ResponseEntity<?> getCandidates(
            @RequestParam String recipientId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String vehicleId
    ) {
        if (recipientId == null || recipientId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "recipientId is required."));
        }
        List<MatchingService.MatchEvaluationDto> candidates =
                matchingService.findMatchesForRecipient(recipientId, category, vehicleId);
        return ResponseEntity.ok(candidates);
    }

    @PostMapping("/evaluate")
    public ResponseEntity<?> evaluateMatch(@RequestBody Map<String, String> body) {
        String batchId = body.get("batchId");
        String recipientId = body.get("recipientId");
        String vehicleId = body.get("vehicleId");

        if (batchId == null || recipientId == null || vehicleId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "batchId, recipientId, and vehicleId are required."));
        }

        MatchResult res = matchingService.evaluateMatch(batchId, recipientId, vehicleId);
        return ResponseEntity.ok(Map.of(
                "accepted", res.isAccepted(),
                "reason", res.getReason(),
                "allocatedPortions", res.getAllocatedPortions(),
                "travelTimeMinutes", res.getTravelTimeMinutes(),
                "handoverMinutes", res.getHandoverMinutes(),
                "route", res.getRouteResult() != null ? res.getRouteResult().getFormattedRoute() : "N/A",
                "pickupTime", res.getPickupTime() != null ? res.getPickupTime().toString().substring(0, 5) : "N/A",
                "expectedCompletionTime", res.getExpectedCompletionTime() != null ? res.getExpectedCompletionTime().toString().substring(0, 5) : "N/A",
                "useByDeadline", res.getUseByDeadline() != null ? res.getUseByDeadline().toString().substring(0, 5) : "N/A"
        ));
    }
}
