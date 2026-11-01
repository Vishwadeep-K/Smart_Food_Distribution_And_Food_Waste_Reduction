package com.annam;

import com.annam.dto.*;
import com.annam.graph.Location;
import com.annam.graph.PathResult;
import com.annam.model.*;
import com.annam.repository.*;
import com.annam.service.AuthService;
import com.annam.service.DonorPortalService;
import com.annam.service.DonorReportService;
import com.annam.service.FoodBatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class DonorPortalAndReportsTests {

    private UserRepository userRepository;
    private DonorRepository donorRepository;
    private FoodBatchRepository batchRepository;
    private AllocationRepository allocationRepository;
    private LocationRepository locationRepository;
    private FoodBatchService foodBatchService;
    private DonorPortalService donorPortalService;
    private DonorReportService donorReportService;
    private AuthService authService;

    private User sampleDonorUser;
    private Donor sampleDonor;
    private Location sampleLocation;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        donorRepository = mock(DonorRepository.class);
        batchRepository = mock(FoodBatchRepository.class);
        allocationRepository = mock(AllocationRepository.class);
        locationRepository = mock(LocationRepository.class);

        foodBatchService = new FoodBatchService(batchRepository, null);
        donorPortalService = new DonorPortalService(userRepository, donorRepository, batchRepository,
                foodBatchService, locationRepository, allocationRepository);
        donorReportService = new DonorReportService(donorRepository, batchRepository, donorPortalService);
        authService = new AuthService(userRepository, donorRepository, locationRepository);

        sampleLocation = new Location("LOC01", "Central Hub", "Transit");
        sampleDonor = new Donor("D01", "Green Feast Catering", "Green Feast", "Restaurant",
                "+91-98401-11001", "Central Chennai", sampleLocation);
        sampleDonorUser = new User(10, "greenfeast@annam.org", "encodedPassword", UserRole.DONOR,
                "Green Feast Catering", "+91-98401-11001", "D01", "Central Hub", LocalDateTime.now());
    }

    @Test
    @DisplayName("Test 1: Food Donor Registration with full fields assigns DONOR role and stores donor type and city")
    void test01_DonorRegistrationSuccess() {
        when(userRepository.existsByEmail("newdonor@test.org")).thenReturn(false);
        when(locationRepository.findById("LOC01")).thenReturn(sampleLocation);
        when(donorRepository.getNextDonorId()).thenReturn("D07");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        RegisterRequest req = new RegisterRequest();
        req.setFullName("Arusuvai Canteen");
        req.setOrganization("Arusuvai Caterers");
        req.setDonorType("Restaurant");
        req.setCity("T. Nagar");
        req.setEmail("newdonor@test.org");
        req.setPhone("+91-98401-77889");
        req.setLocationId("LOC01");
        req.setPickupAddress("Shop 4, South Usman Road");
        req.setPassword("Secure@2026");
        req.setConfirmPassword("Secure@2026");

        UserDto created = authService.registerDonor(req);

        assertNotNull(created);
        assertEquals("DONOR", created.getRole());
        assertEquals("D07", created.getDonorId());
        assertEquals("newdonor@test.org", created.getEmail());

        verify(donorRepository).save(argThat(d ->
                "D07".equals(d.getDonorId()) &&
                "Restaurant".equals(d.getDonorType()) &&
                "T. Nagar".equals(d.getCity())
        ));
    }

    @Test
    @DisplayName("Test 2: Donor Registration fails on missing required fields or mismatched password")
    void test02_DonorRegistrationValidationFailures() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Test");
        req.setEmail("invalid-email");
        req.setPassword("short");
        req.setConfirmPassword("mismatch");

        assertThrows(IllegalArgumentException.class, () -> authService.registerDonor(req));

        req.setEmail("valid@test.org");
        req.setPassword("Valid@123");
        req.setConfirmPassword("Valid@123");
        // Missing donorType & city
        assertThrows(IllegalArgumentException.class, () -> authService.registerDonor(req));
    }

    @Test
    @DisplayName("Test 3: Donor Profile computes all 5 summary cards from database records")
    void test03_DonorProfileComputesFiveSummaryCards() {
        when(donorRepository.findById("D01")).thenReturn(sampleDonor);

        // 1. Pending Approval Batch (50 portions)
        FoodBatch b1 = new FoodBatch("B101", FoodCategory.MEALS, "Meals", 50, 50, 0, 0,
                LocalTime.of(10, 0), LocalTime.of(14, 0), sampleDonor, sampleLocation, "Notes",
                ApprovalStatus.NOT_APPROVED, false, LocalDateTime.now());

        // 2. Delivered Batch (30 portions delivered)
        FoodBatch b2 = new FoodBatch("B102", FoodCategory.BIRYANI, "Biryani", 30, 0, 0, 30,
                LocalTime.of(11, 0), LocalTime.of(15, 0), sampleDonor, sampleLocation, "Notes",
                ApprovalStatus.APPROVED, false, LocalDateTime.now());

        // 3. Expired Batch (20 portions expired)
        FoodBatch b3 = new FoodBatch("B103", FoodCategory.RICE, "Rice", 20, 0, 0, 0,
                LocalTime.of(8, 0), LocalTime.of(11, 0), sampleDonor, sampleLocation, "Notes",
                ApprovalStatus.APPROVED, true, LocalDateTime.now());

        when(batchRepository.findByDonorId("D01")).thenReturn(List.of(b1, b2, b3));

        DonorProfileDto profile = donorPortalService.getDonorProfile(sampleDonorUser);

        assertNotNull(profile);
        assertEquals(3, profile.getTotalDonationsCount(), "Total donations submitted should be 3");
        assertEquals(100, profile.getTotalPortionsDonated(), "Total portions should be 50 + 30 + 20 = 100");
        assertEquals(1, profile.getPendingApprovalCount(), "Pending approval count should be 1");
        assertEquals(1, profile.getDeliveredDonationsCount(), "Delivered batches count should be 1");
        assertEquals(30, profile.getDeliveredPortionsCount(), "Delivered portions should be 30");
        assertEquals(1, profile.getExpiredOrRejectedCount(), "Expired/rejected count should be 1");
        assertEquals("Restaurant", profile.getDonorType());
        assertEquals("Central Chennai", profile.getCity());
    }

    @Test
    @DisplayName("Test 4: Submitting food donation creates record with initial NOT_APPROVED status")
    void test04_SubmitDonationInitialPendingApproval() {
        when(donorRepository.findById("D01")).thenReturn(sampleDonor);
        when(locationRepository.findById("LOC01")).thenReturn(sampleLocation);
        when(batchRepository.getNextBatchId()).thenReturn("B88");
        when(batchRepository.findById("B88")).thenReturn(null);

        SubmitDonationRequest sub = new SubmitDonationRequest();
        sub.setFoodName("Vegetable Fried Rice");
        sub.setCategory("RICE");
        sub.setTotalPortions(45);
        sub.setPreparationTime("11:30");
        sub.setUseByTime("15:30");
        sub.setPickupLocationId("LOC01");
        sub.setNotes("Hot in thermal box");

        DonorDonationDto result = donorPortalService.submitDonation(sampleDonorUser, sub);

        assertNotNull(result);
        assertEquals("B88", result.getBatchId());
        assertEquals("NOT_APPROVED", result.getApprovalStatus());
        assertEquals("Pending Approval", result.getDeliveryStatus());

        verify(batchRepository).save(argThat(b ->
                "B88".equals(b.getBatchId()) &&
                ApprovalStatus.NOT_APPROVED == b.getApprovalStatus() &&
                b.getTotalPortions() == 45
        ));
    }

    @Test
    @DisplayName("Test 5: My Donations view displays status indicators and rejection reason")
    void test05_MyDonationsStatusIndicators() {
        FoodBatch b1 = new FoodBatch("B01", FoodCategory.MEALS, "Veg Meals", 40, 40, 0, 0,
                LocalTime.of(10, 0), LocalTime.of(14, 0), sampleDonor, sampleLocation, "Notes",
                "Packaging broken", ApprovalStatus.REJECTED, false, LocalDateTime.now());

        DonorDonationDto dto = donorPortalService.toDonationDto(b1);
        assertEquals("Rejected", dto.getDeliveryStatus());
        assertEquals("Packaging broken", dto.getRejectionReason());

        FoodBatch b2 = new FoodBatch("B02", FoodCategory.CHAPATI, "Rotis", 50, 20, 30, 0,
                LocalTime.of(11, 0), LocalTime.of(15, 0), sampleDonor, sampleLocation, "Notes",
                ApprovalStatus.APPROVED, false, LocalDateTime.now());

        DonorDonationDto dto2 = donorPortalService.toDonationDto(b2);
        assertEquals("Partially Allocated", dto2.getDeliveryStatus());
        assertEquals(20, dto2.getRemainingPortions());
    }

    @Test
    @DisplayName("Test 6: Detailed view includes recipient and vehicle breakdown for allocated batches")
    void test06_DetailedBreakdownViewWithAllocations() {
        FoodBatch b1 = new FoodBatch("B01", FoodCategory.MEALS, "Meals", 50, 20, 30, 0,
                LocalTime.of(10, 0), LocalTime.of(14, 0), sampleDonor, sampleLocation, "Notes",
                ApprovalStatus.APPROVED, false, LocalDateTime.now());

        when(batchRepository.findById("B01")).thenReturn(b1);

        Recipient recipient = new Shelter("R01", "Hope Haven Shelter", sampleLocation, 100);
        DeliveryVehicle vehicle = new DeliveryVehicle("V01", "Green Van", sampleLocation, 100, true);
        PathResult path = PathResult.success(sampleLocation, sampleLocation, List.of(sampleLocation), 15);
        FoodAllocation alloc = new FoodAllocation("A01", b1, recipient, vehicle, 30, path,
                LocalTime.of(12, 0), 10, AllocationStatus.RESERVED);

        when(allocationRepository.findByBatchId("B01")).thenReturn(List.of(alloc));

        DonorDonationDto detail = donorPortalService.getDonationById(sampleDonorUser, "B01");

        assertNotNull(detail);
        assertEquals(20, detail.getRemainingPortions());
        assertEquals(1, detail.getAllocations().size());
        assertEquals("A01", detail.getAllocations().get(0).getAllocationId());
        assertEquals("Hope Haven Shelter", detail.getAllocations().get(0).getRecipientName());
        assertEquals("Green Van", detail.getAllocations().get(0).getVehicleName());
    }

    @Test
    @DisplayName("Test 7: IDOR Protection: Donor cannot view another donor's donation record")
    void test07_IdorProtectionPreventsAccessToOtherDonorBatches() {
        Donor otherDonor = new Donor("D02", "Other Restaurant", "Other", "555", sampleLocation);
        FoodBatch otherBatch = new FoodBatch("B99", FoodCategory.MEALS, "Secret", 30, 30, 0, 0,
                LocalTime.of(10, 0), LocalTime.of(14, 0), otherDonor, sampleLocation, "Notes",
                ApprovalStatus.APPROVED, false, LocalDateTime.now());

        when(batchRepository.findById("B99")).thenReturn(otherBatch);

        // sampleDonorUser has donorId = "D01", requesting batch "B99" belonging to "D02"
        assertThrows(IllegalArgumentException.class, () ->
                donorPortalService.getDonationById(sampleDonorUser, "B99")
        );
    }

    @Test
    @DisplayName("Test 8: Donor Contribution Report generates correct statistics and category breakdown")
    void test08_DonorReportSummaryGeneration() {
        when(donorRepository.findById("D01")).thenReturn(sampleDonor);

        LocalDateTime now = LocalDateTime.now();
        FoodBatch b1 = new FoodBatch("B01", FoodCategory.MEALS, "Meals", 50, 0, 0, 50,
                LocalTime.of(10, 0), LocalTime.of(14, 0), sampleDonor, sampleLocation, "Notes",
                ApprovalStatus.APPROVED, false, now);
        FoodBatch b2 = new FoodBatch("B02", FoodCategory.BIRYANI, "Biryani", 30, 0, 0, 30,
                LocalTime.of(10, 0), LocalTime.of(14, 0), sampleDonor, sampleLocation, "Notes",
                ApprovalStatus.APPROVED, false, now);
        FoodBatch b3 = new FoodBatch("B03", FoodCategory.MEALS, "Meals", 20, 0, 0, 0,
                LocalTime.of(8, 0), LocalTime.of(10, 0), sampleDonor, sampleLocation, "Notes",
                ApprovalStatus.APPROVED, true, now);

        when(batchRepository.findByDonorId("D01")).thenReturn(List.of(b1, b2, b3));

        DonorReportDto report = donorReportService.generateDonorReport(sampleDonorUser, "all", null, null);

        assertNotNull(report);
        assertEquals(3, report.getTotalDonationsSubmitted());
        assertEquals(100, report.getTotalPortionsDonated());
        assertEquals(80, report.getTotalPortionsDelivered());
        assertEquals(80.0, report.getDeliverySuccessRate(), 0.01);
        assertEquals(2, report.getCategoryBreakdown().size());
        assertEquals(1, report.getExpiredDonationsCount());
    }

    @Test
    @DisplayName("Test 9: CSV Report Export produces valid RFC-4180 format with UTF-8 BOM")
    void test09_CsvReportExportFormat() {
        when(donorRepository.findById("D01")).thenReturn(sampleDonor);

        FoodBatch b1 = new FoodBatch("B01", FoodCategory.MEALS, "Cooked Veg Meals", 50, 0, 0, 50,
                LocalTime.of(10, 0), LocalTime.of(14, 0), sampleDonor, sampleLocation, "None",
                ApprovalStatus.APPROVED, false, LocalDateTime.now());

        when(batchRepository.findByDonorId("D01")).thenReturn(List.of(b1));

        DonorReportDto report = donorReportService.generateDonorReport(sampleDonorUser, "all", null, null);
        byte[] csvBytes = donorReportService.generateCsvReport(report);

        assertNotNull(csvBytes);
        assertTrue(csvBytes.length > 0);
        // Verify UTF-8 BOM prefix
        assertEquals((byte) 0xEF, csvBytes[0]);
        assertEquals((byte) 0xBB, csvBytes[1]);
        assertEquals((byte) 0xBF, csvBytes[2]);

        String csvString = new String(csvBytes, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(csvString.contains("ANNAM Donor Contribution Report"));
        assertTrue(csvString.contains("Batch ID"));
        assertTrue(csvString.contains("B01"));
        assertTrue(csvString.contains("Cooked Veg Meals"));
    }

    @Test
    @DisplayName("Test 10: PDF Report Export produces valid PDF document bytes with header")
    void test10_PdfReportExportFormat() {
        when(donorRepository.findById("D01")).thenReturn(sampleDonor);

        FoodBatch b1 = new FoodBatch("B01", FoodCategory.MEALS, "Cooked Meals", 50, 0, 0, 50,
                LocalTime.of(10, 0), LocalTime.of(14, 0), sampleDonor, sampleLocation, "None",
                ApprovalStatus.APPROVED, false, LocalDateTime.now());

        when(batchRepository.findByDonorId("D01")).thenReturn(List.of(b1));

        DonorReportDto report = donorReportService.generateDonorReport(sampleDonorUser, "all", null, null);
        byte[] pdfBytes = donorReportService.generatePdfReport(report);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 100);
        // Standard PDF magic byte header check: %PDF-
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }

    @Test
    @DisplayName("Test 11: Admin Approvals: Batch approval updates PriorityQueue; Rejection saves reason")
    void test11_AdminApprovalAndRejectionOperations() {
        FoodBatch b1 = new FoodBatch("B55", FoodCategory.BIRYANI, "Veg Biryani", 30, 30, 0, 0,
                LocalTime.of(11, 0), LocalTime.of(15, 0), sampleDonor, sampleLocation, "Notes",
                ApprovalStatus.NOT_APPROVED, false, LocalDateTime.now());

        when(batchRepository.findAll()).thenReturn(new ArrayList<>(List.of(b1)));
        foodBatchService.reloadFromDatabase();

        assertEquals(0, foodBatchService.getApprovedPriorityQueueOrder().size());

        // 1. Admin Approves
        foodBatchService.approveBatch("B55");
        assertEquals(ApprovalStatus.APPROVED, b1.getApprovalStatus());
        assertEquals(1, foodBatchService.getApprovedPriorityQueueOrder().size());
        verify(batchRepository).updateApproval("B55", ApprovalStatus.APPROVED);

        // 2. Admin Rejects with reason
        foodBatchService.rejectBatch("B55", "Insufficient shelf life remaining");
        assertEquals(ApprovalStatus.REJECTED, b1.getApprovalStatus());
        assertEquals("Insufficient shelf life remaining", b1.getRejectionReason());
        assertEquals(0, foodBatchService.getApprovedPriorityQueueOrder().size());
        verify(batchRepository).updateRejection("B55", "Insufficient shelf life remaining");
    }
}
