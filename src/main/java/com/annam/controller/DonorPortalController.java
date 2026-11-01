package com.annam.controller;

import com.annam.dto.DonorDonationDto;
import com.annam.dto.DonorProfileDto;
import com.annam.dto.SubmitDonationRequest;
import com.annam.model.User;
import com.annam.service.AuthService;
import com.annam.service.DonorPortalService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/donor")
public class DonorPortalController {

    private final AuthService authService;
    private final DonorPortalService donorPortalService;
    private final com.annam.service.DonorReportService donorReportService;

    public DonorPortalController(AuthService authService, DonorPortalService donorPortalService,
                                 com.annam.service.DonorReportService donorReportService) {
        this.authService = authService;
        this.donorPortalService = donorPortalService;
        this.donorReportService = donorReportService;
    }

    private User getAuthenticatedDonor(HttpSession session) {
        Optional<User> userOpt = authService.getCurrentUser(session);
        if (userOpt.isEmpty()) {
            return null;
        }
        User user = userOpt.get();
        if (user.getDonorId() == null) {
            return null;
        }
        return user;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpSession session) {
        User user = getAuthenticatedDonor(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Authentication required. Please log in as a Donor."
            ));
        }

        try {
            DonorProfileDto profile = donorPortalService.getDonorProfile(user);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body, HttpSession session) {
        User user = getAuthenticatedDonor(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Authentication required. Please log in as a Donor."
            ));
        }

        try {
            String name = body.get("name");
            String phone = body.get("phone");
            String pickupAddress = body.get("pickupAddress");
            String locationId = body.get("locationId");

            DonorProfileDto updated = donorPortalService.updateDonorProfile(user, name, phone, pickupAddress, locationId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profile updated successfully.",
                    "profile", updated
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/donations")
    public ResponseEntity<?> submitDonation(@RequestBody SubmitDonationRequest request, HttpSession session) {
        User user = getAuthenticatedDonor(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Authentication required. Please log in as a Donor."
            ));
        }

        try {
            DonorDonationDto donation = donorPortalService.submitDonation(user, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Food donation " + donation.getBatchId() + " submitted successfully! Status: Pending Admin Approval.",
                    "donation", donation
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Error submitting donation: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/donations")
    public ResponseEntity<?> getDonations(HttpSession session) {
        User user = getAuthenticatedDonor(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Authentication required. Please log in as a Donor."
            ));
        }

        try {
            List<DonorDonationDto> list = donorPortalService.getDonationsForDonor(user);
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/donations/{id}")
    public ResponseEntity<?> getDonationDetail(@PathVariable String id, HttpSession session) {
        User user = getAuthenticatedDonor(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Authentication required. Please log in as a Donor."
            ));
        }

        try {
            DonorDonationDto dto = donorPortalService.getDonationById(user, id);
            if (dto == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Donation not found."));
            }
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/reports/summary")
    public ResponseEntity<?> getReportSummary(
            @RequestParam(required = false, defaultValue = "all") String range,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpSession session
    ) {
        User user = getAuthenticatedDonor(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Authentication required. Please log in as a Donor."
            ));
        }

        try {
            com.annam.dto.DonorReportDto report = donorReportService.generateDonorReport(user, range, startDate, endDate);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/reports/export/csv")
    public ResponseEntity<?> exportReportCsv(
            @RequestParam(required = false, defaultValue = "all") String range,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpSession session
    ) {
        User user = getAuthenticatedDonor(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Authentication required. Please log in as a Donor."
            ));
        }

        try {
            com.annam.dto.DonorReportDto report = donorReportService.generateDonorReport(user, range, startDate, endDate);
            byte[] csvBytes = donorReportService.generateCsvReport(report);

            String filename = "ANNAM_Donor_Contribution_Report_" + user.getDonorId() + "_" + System.currentTimeMillis() + ".csv";
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .header("Content-Type", "text/csv; charset=UTF-8")
                    .body(csvBytes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/reports/export/pdf")
    public ResponseEntity<?> exportReportPdf(
            @RequestParam(required = false, defaultValue = "all") String range,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpSession session
    ) {
        User user = getAuthenticatedDonor(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Authentication required. Please log in as a Donor."
            ));
        }

        try {
            com.annam.dto.DonorReportDto report = donorReportService.generateDonorReport(user, range, startDate, endDate);
            byte[] pdfBytes = donorReportService.generatePdfReport(report);

            String filename = "ANNAM_Donor_Contribution_Report_" + user.getDonorId() + "_" + System.currentTimeMillis() + ".pdf";
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .header("Content-Type", "application/pdf")
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
