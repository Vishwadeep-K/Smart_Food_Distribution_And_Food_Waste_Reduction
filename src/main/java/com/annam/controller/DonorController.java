package com.annam.controller;

import com.annam.graph.Location;
import com.annam.model.Donor;
import com.annam.service.DonorService;
import com.annam.service.RoadNetworkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/donors")
public class DonorController {
    private final DonorService donorService;
    private final RoadNetworkService roadNetworkService;
    private final com.annam.repository.FoodBatchRepository foodBatchRepository;

    public DonorController(DonorService donorService, RoadNetworkService roadNetworkService,
                           com.annam.repository.FoodBatchRepository foodBatchRepository) {
        this.donorService = donorService;
        this.roadNetworkService = roadNetworkService;
        this.foodBatchRepository = foodBatchRepository;
    }

    @GetMapping
    public ResponseEntity<List<DonorDto>> getAllDonors() {
        List<Donor> list = donorService.getAllDonors();
        List<DonorDto> dtos = list.stream().map(d -> new DonorDto(
                d.getDonorId(),
                d.getName(),
                d.getOrganization(),
                d.getDonorType(),
                d.getPhone(),
                d.getCity(),
                d.getLocation().getLocationId(),
                d.getLocation().getName(),
                donorService.getDonationBatchCount(d.getDonorId())
        )).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDonorById(@PathVariable String id) {
        Donor d = donorService.getDonorById(id);
        if (d == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new DonorDto(
                d.getDonorId(), d.getName(), d.getOrganization(), d.getDonorType(),
                d.getPhone(), d.getCity(),
                d.getLocation().getLocationId(), d.getLocation().getName(),
                donorService.getDonationBatchCount(d.getDonorId())
        ));
    }

    @GetMapping("/{id}/donations")
    public ResponseEntity<?> getDonorDonations(@PathVariable String id) {
        Donor d = donorService.getDonorById(id);
        if (d == null) {
            return ResponseEntity.notFound().build();
        }
        var batches = foodBatchRepository.findByDonorId(id);
        return ResponseEntity.ok(batches);
    }

    @PostMapping
    public ResponseEntity<?> createDonor(@RequestBody Map<String, String> body) {
        String id = body.get("donorId");
        String name = body.get("name");
        String org = body.get("organization");
        String donorType = body.getOrDefault("donorType", "Restaurant");
        String phone = body.get("phone");
        String city = body.getOrDefault("city", "");
        String locId = body.get("locationId");

        if (id == null || id.trim().isEmpty() || name == null || name.trim().isEmpty() || locId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Donor ID, Name, and Location are required."));
        }

        Location loc = roadNetworkService.getLocation(locId);
        if (loc == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid Location ID: " + locId));
        }

        Donor donor = new Donor(id, name, org, donorType, phone, city, loc);
        donorService.saveDonor(donor);
        return ResponseEntity.ok(Map.of("success", true, "message", "Donor created successfully.", "donorId", id.toUpperCase()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDonor(@PathVariable String id, @RequestBody Map<String, String> body) {
        Donor existing = donorService.getDonorById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        String name = body.get("name");
        String org = body.get("organization");
        String donorType = body.get("donorType");
        String phone = body.get("phone");
        String city = body.get("city");
        String locId = body.get("locationId");

        if (name != null && !name.trim().isEmpty()) existing.setName(name);
        if (org != null) existing.setOrganization(org);
        if (donorType != null) existing.setDonorType(donorType);
        if (phone != null) existing.setPhone(phone);
        if (city != null) existing.setCity(city);
        if (locId != null) {
            Location loc = roadNetworkService.getLocation(locId);
            if (loc != null) existing.setLocation(loc);
        }

        donorService.saveDonor(existing);
        return ResponseEntity.ok(Map.of("success", true, "message", "Donor updated successfully."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDonor(@PathVariable String id) {
        donorService.deleteDonor(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Donor deleted successfully."));
    }

    public record DonorDto(String donorId, String name, String organization, String donorType,
                           String phone, String city, String locationId, String locationName, int batchCount) {}
}
