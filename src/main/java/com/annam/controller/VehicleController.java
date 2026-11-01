package com.annam.controller;

import com.annam.graph.Location;
import com.annam.model.DeliveryVehicle;
import com.annam.service.RoadNetworkService;
import com.annam.service.VehicleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
    private final VehicleService vehicleService;
    private final RoadNetworkService roadNetworkService;

    public VehicleController(VehicleService vehicleService, RoadNetworkService roadNetworkService) {
        this.vehicleService = vehicleService;
        this.roadNetworkService = roadNetworkService;
    }

    @GetMapping
    public ResponseEntity<List<VehicleDto>> getAllVehicles() {
        List<DeliveryVehicle> list = vehicleService.getAllVehicles();
        List<VehicleDto> dtos = list.stream().map(v -> new VehicleDto(
                v.getVehicleId(),
                v.getName(),
                v.getCurrentLocation().getLocationId(),
                v.getCurrentLocation().getName(),
                v.getPortionCapacity(),
                v.isAvailable()
        )).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getVehicleById(@PathVariable String id) {
        DeliveryVehicle v = vehicleService.getVehicleById(id);
        if (v == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new VehicleDto(
                v.getVehicleId(), v.getName(),
                v.getCurrentLocation().getLocationId(), v.getCurrentLocation().getName(),
                v.getPortionCapacity(), v.isAvailable()
        ));
    }

    @PostMapping
    public ResponseEntity<?> createVehicle(@RequestBody Map<String, Object> body) {
        String id = (String) body.get("vehicleId");
        String name = (String) body.get("name");
        String locId = (String) body.get("currentLocationId");
        Number capNum = (Number) body.get("portionCapacity");
        int cap = capNum != null ? capNum.intValue() : 0;

        if (id == null || name == null || locId == null || cap <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "ID, Name, Location, and Capacity (> 0) are required."));
        }

        Location loc = roadNetworkService.getLocation(locId);
        if (loc == null) return ResponseEntity.badRequest().body(Map.of("message", "Location not found: " + locId));

        DeliveryVehicle vehicle = new DeliveryVehicle(id, name, loc, cap, true);
        vehicleService.saveVehicle(vehicle);
        return ResponseEntity.ok(Map.of("success", true, "message", "Vehicle created successfully.", "vehicleId", id.toUpperCase()));
    }

    @PutMapping("/{id}/availability")
    public ResponseEntity<?> updateAvailability(@PathVariable String id, @RequestBody Map<String, Boolean> body) {
        Boolean available = body.get("isAvailable");
        if (available == null) return ResponseEntity.badRequest().body(Map.of("message", "isAvailable field is required."));

        vehicleService.setAvailable(id, available);
        return ResponseEntity.ok(Map.of("success", true, "message", "Vehicle availability updated."));
    }

    public record VehicleDto(String vehicleId, String name, String currentLocationId,
                             String currentLocationName, int portionCapacity, boolean isAvailable) {}
}
