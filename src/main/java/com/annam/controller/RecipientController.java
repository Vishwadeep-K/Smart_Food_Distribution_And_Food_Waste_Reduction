package com.annam.controller;

import com.annam.graph.Location;
import com.annam.model.CommunityCentre;
import com.annam.model.FoodCategory;
import com.annam.model.Recipient;
import com.annam.model.Shelter;
import com.annam.service.RecipientService;
import com.annam.service.RoadNetworkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/recipients")
public class RecipientController {
    private final RecipientService recipientService;
    private final RoadNetworkService roadNetworkService;

    public RecipientController(RecipientService recipientService, RoadNetworkService roadNetworkService) {
        this.recipientService = recipientService;
        this.roadNetworkService = roadNetworkService;
    }

    @GetMapping
    public ResponseEntity<List<RecipientDto>> getAllRecipients() {
        List<Recipient> list = recipientService.getAllRecipients();
        List<RecipientDto> dtos = list.stream().map(r -> new RecipientDto(
                r.getRecipientId(),
                r.getName(),
                r.getRecipientType(),
                r.getLocation().getLocationId(),
                r.getLocation().getName(),
                r.getTotalRequiredPortions(),
                r.getRemainingDemandPortions(),
                r.getAcceptedCategories().stream().map(Enum::name).toList()
        )).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRecipientById(@PathVariable String id) {
        Recipient r = recipientService.getRecipientById(id);
        if (r == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new RecipientDto(
                r.getRecipientId(), r.getName(), r.getRecipientType(),
                r.getLocation().getLocationId(), r.getLocation().getName(),
                r.getTotalRequiredPortions(), r.getRemainingDemandPortions(),
                r.getAcceptedCategories().stream().map(Enum::name).toList()
        ));
    }

    @PostMapping
    public ResponseEntity<?> createRecipient(@RequestBody Map<String, Object> body) {
        String id = (String) body.get("recipientId");
        String name = (String) body.get("name");
        String type = (String) body.get("recipientType");
        String locId = (String) body.get("locationId");
        Number portionsNum = (Number) body.get("totalRequiredPortions");
        int portions = portionsNum != null ? portionsNum.intValue() : 0;

        if (id == null || name == null || locId == null || portions <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "ID, Name, Location, and Portions (> 0) are required."));
        }

        Location loc = roadNetworkService.getLocation(locId);
        if (loc == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid Location ID: " + locId));
        }

        Recipient recipient;
        if ("Community Centre".equalsIgnoreCase(type) || "CommunityCentre".equalsIgnoreCase(type)) {
            recipient = new CommunityCentre(id, name, loc, portions);
        } else {
            recipient = new Shelter(id, name, loc, portions);
        }

        if (body.get("acceptedCategories") instanceof List<?> catList) {
            Set<FoodCategory> set = new HashSet<>();
            for (Object obj : catList) {
                try {
                    set.add(FoodCategory.valueOf(obj.toString().toUpperCase()));
                } catch (Exception ignored) {}
            }
            if (!set.isEmpty()) {
                recipient.setAcceptedCategories(set);
            }
        }

        recipientService.saveRecipient(recipient);
        return ResponseEntity.ok(Map.of("success", true, "message", "Recipient created successfully.", "recipientId", id.toUpperCase()));
    }

    @PutMapping("/{id}/demand")
    public ResponseEntity<?> updateDemand(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        Integer demand = body.get("remainingDemand");
        if (demand == null || demand < 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid demand portion value."));
        }
        recipientService.updateDemand(id, demand);
        return ResponseEntity.ok(Map.of("success", true, "message", "Demand updated successfully."));
    }

    public record RecipientDto(String recipientId, String name, String recipientType,
                               String locationId, String locationName, int totalRequiredPortions,
                               int remainingDemandPortions, List<String> acceptedCategories) {}
}
