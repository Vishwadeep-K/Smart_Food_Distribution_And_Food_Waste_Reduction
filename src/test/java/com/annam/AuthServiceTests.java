package com.annam;

import com.annam.config.RoleAuthorizationInterceptor;
import com.annam.dto.LoginRequest;
import com.annam.dto.RegisterRequest;
import com.annam.dto.UserDto;
import com.annam.graph.Location;
import com.annam.model.ApprovalStatus;
import com.annam.model.Donor;
import com.annam.model.FoodBatch;
import com.annam.model.FoodCategory;
import com.annam.model.User;
import com.annam.model.UserRole;
import com.annam.repository.AllocationRepository;
import com.annam.repository.DonorRepository;
import com.annam.repository.FoodBatchRepository;
import com.annam.repository.LocationRepository;
import com.annam.repository.UserRepository;
import com.annam.service.AuthService;
import com.annam.service.FoodBatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class AuthServiceTests {

    private UserRepository userRepository;
    private DonorRepository donorRepository;
    private LocationRepository locationRepository;
    private AuthService authService;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        donorRepository = mock(DonorRepository.class);
        locationRepository = mock(LocationRepository.class);
        authService = new AuthService(userRepository, donorRepository, locationRepository);
        passwordEncoder = new BCryptPasswordEncoder();
    }

    @Test
    @DisplayName("Scenario 1: Admin login with valid credentials succeeds and returns ADMIN role")
    void test01_AdminLoginSuccess() {
        String rawPassword = "AdminPassword@123";
        String encoded = passwordEncoder.encode(rawPassword);

        User adminUser = new User(1, "admin@annam.org", encoded, UserRole.ADMIN,
                "System Administrator", "+91-98401-00000", null, "Central Hub", LocalDateTime.now());

        when(userRepository.findByEmail("admin@annam.org")).thenReturn(Optional.of(adminUser));

        MockHttpSession session = new MockHttpSession();
        LoginRequest req = new LoginRequest("admin@annam.org", rawPassword, "ADMIN");

        UserDto result = authService.authenticate(req, session);

        assertNotNull(result);
        assertEquals("ADMIN", result.getRole());
        assertEquals("admin@annam.org", result.getEmail());
        assertEquals(adminUser, session.getAttribute(AuthService.SESSION_USER_KEY));
    }

    @Test
    @DisplayName("Scenario 2: Donor login with valid credentials succeeds and returns DONOR role")
    void test02_DonorLoginSuccess() {
        String rawPassword = "DonorPassword@123";
        String encoded = passwordEncoder.encode(rawPassword);

        User donorUser = new User(2, "palace@annam.org", encoded, UserRole.DONOR,
                "Grand Palace Hotel", "+91-98401-11001", "D01", "Central Hub", LocalDateTime.now());

        when(userRepository.findByEmail("palace@annam.org")).thenReturn(Optional.of(donorUser));

        MockHttpSession session = new MockHttpSession();
        LoginRequest req = new LoginRequest("palace@annam.org", rawPassword, "DONOR");

        UserDto result = authService.authenticate(req, session);

        assertNotNull(result);
        assertEquals("DONOR", result.getRole());
        assertEquals("D01", result.getDonorId());
        assertEquals(donorUser, session.getAttribute(AuthService.SESSION_USER_KEY));
    }

    @Test
    @DisplayName("Scenario 3: Invalid email or incorrect password throws IllegalArgumentException")
    void test03_InvalidCredentialsThrows() {
        when(userRepository.findByEmail("unknown@annam.org")).thenReturn(Optional.empty());

        MockHttpSession session = new MockHttpSession();
        LoginRequest reqWrongEmail = new LoginRequest("unknown@annam.org", "anyPass", "ADMIN");

        assertThrows(IllegalArgumentException.class, () -> {
            authService.authenticate(reqWrongEmail, session);
        }, "Should reject unknown email");

        // Wrong password test
        String encoded = passwordEncoder.encode("RealPassword@123");
        User adminUser = new User(1, "admin@annam.org", encoded, UserRole.ADMIN,
                "System Administrator", "+91-98401-00000", null, "Central Hub", LocalDateTime.now());

        when(userRepository.findByEmail("admin@annam.org")).thenReturn(Optional.of(adminUser));

        LoginRequest reqWrongPass = new LoginRequest("admin@annam.org", "WrongPass@999", "ADMIN");
        assertThrows(IllegalArgumentException.class, () -> {
            authService.authenticate(reqWrongPass, session);
        }, "Should reject incorrect password");
    }

    @Test
    @DisplayName("Scenario 4: Donor registration with duplicate email is strictly rejected")
    void test04_DuplicateEmailRegistrationRejected() {
        when(userRepository.existsByEmail("existing@annam.org")).thenReturn(true);

        RegisterRequest req = new RegisterRequest();
        req.setFullName("Duplicate Kitchen");
        req.setEmail("existing@annam.org");
        req.setPhone("+91-98765-43210");
        req.setPassword("Secret@123");
        req.setConfirmPassword("Secret@123");

        assertThrows(IllegalArgumentException.class, () -> {
            authService.registerDonor(req);
        }, "Should strictly reject registration with duplicate email address");
    }

    @Test
    @DisplayName("Scenario 5: New registrations are always assigned DONOR role (never ADMIN)")
    void test05_NewRegistrationsAlwaysDonorRole() {
        when(userRepository.existsByEmail("newdonor@annam.org")).thenReturn(false);
        when(locationRepository.findById("LOC01")).thenReturn(new Location("LOC01", "Central Hub", "Transit"));
        when(donorRepository.getNextDonorId()).thenReturn("D06");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterRequest req = new RegisterRequest();
        req.setFullName("Sunrise Bakers");
        req.setDonorType("Institution");
        req.setCity("Central");
        req.setEmail("newdonor@annam.org");
        req.setPhone("+91-98401-99999");
        req.setLocationId("LOC01");
        req.setPickupAddress("Central Hub 12");
        req.setPassword("StrongPass@123");
        req.setConfirmPassword("StrongPass@123");

        UserDto created = authService.registerDonor(req);

        assertNotNull(created);
        assertEquals("DONOR", created.getRole(), "New registration MUST receive DONOR role");
        assertEquals("D06", created.getDonorId());
    }

    @Test
    @DisplayName("Scenario 6: Role mismatch: Donor attempting to log in on Admin tab is rejected")
    void test06_RoleMismatchRejected() {
        String encoded = passwordEncoder.encode("ValidPass@123");
        User donorUser = new User(2, "donor@annam.org", encoded, UserRole.DONOR,
                "Donor Kitchen", "+91-98401-11001", "D01", "Central Hub", LocalDateTime.now());

        when(userRepository.findByEmail("donor@annam.org")).thenReturn(Optional.of(donorUser));

        MockHttpSession session = new MockHttpSession();
        // Donor credentials entered on ADMIN tab
        LoginRequest req = new LoginRequest("donor@annam.org", "ValidPass@123", "ADMIN");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.authenticate(req, session);
        });

        assertTrue(ex.getMessage().contains("Donor"));
    }

    @Test
    @DisplayName("Scenario 7: RoleAuthorizationInterceptor prevents unauthenticated and donor access to admin endpoints")
    void test07_RoleAuthorizationInterceptorEnforcement() throws Exception {
        RoleAuthorizationInterceptor interceptor = new RoleAuthorizationInterceptor();

        // 1. Unauthenticated request to /api/dashboard
        MockHttpServletRequest req1 = new MockHttpServletRequest("GET", "/api/dashboard");
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        boolean allowed1 = interceptor.preHandle(req1, res1, new Object());
        assertFalse(allowed1, "Unauthenticated user should not access /api/dashboard");
        assertEquals(401, res1.getStatus());

        // 2. Donor attempting to access /api/dashboard -> 403 Forbidden
        MockHttpServletRequest req2 = new MockHttpServletRequest("GET", "/api/dashboard");
        MockHttpSession donorSession = new MockHttpSession();
        donorSession.setAttribute(AuthService.SESSION_USER_KEY,
                new User(2, "donor@annam.org", "hash", UserRole.DONOR, "Donor", "123", "D01", "Hub", LocalDateTime.now()));
        req2.setSession(donorSession);
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        boolean allowed2 = interceptor.preHandle(req2, res2, new Object());
        assertFalse(allowed2, "Donor should be denied access to admin endpoint /api/dashboard");
        assertEquals(403, res2.getStatus());

        // 3. Admin accessing /api/dashboard -> Allowed
        MockHttpServletRequest req3 = new MockHttpServletRequest("GET", "/api/dashboard");
        MockHttpSession adminSession = new MockHttpSession();
        adminSession.setAttribute(AuthService.SESSION_USER_KEY,
                new User(1, "admin@annam.org", "hash", UserRole.ADMIN, "Admin", "123", null, "Hub", LocalDateTime.now()));
        req3.setSession(adminSession);
        MockHttpServletResponse res3 = new MockHttpServletResponse();
        boolean allowed3 = interceptor.preHandle(req3, res3, new Object());
        assertTrue(allowed3, "Admin should be permitted access to /api/dashboard");
    }

    @Test
    @DisplayName("Scenario 6: Donor attempting to view donations only receives their own records (isolated by donorId)")
    void test10_DonorOnlyViewsOwnDonations() {
        FoodBatchRepository batchRepo = mock(FoodBatchRepository.class);
        FoodBatchService batchService = mock(FoodBatchService.class);
        AllocationRepository allocationRepo = mock(AllocationRepository.class);
        com.annam.service.DonorPortalService donorPortalService = 
                new com.annam.service.DonorPortalService(userRepository, donorRepository, batchRepo, batchService, locationRepository, allocationRepo);

        User donor1 = new User(1, "donor1@annam.org", "hash", UserRole.DONOR, "Donor One", "123", "D01", "Hub", LocalDateTime.now());

        Location loc = new Location("LOC01", "Hub", "Transit");
        Donor d01 = new Donor("D01", "Donor One", "Org One", "123", loc);
        FoodBatch b01 = new FoodBatch("B01", FoodCategory.MEALS, "Veg Meals", 30, 30, 0, 0, 
                LocalTime.of(10, 0), LocalTime.of(14, 0), d01, loc, "Hot", ApprovalStatus.APPROVED, false, LocalDateTime.now());

        when(batchRepo.findByDonorId("D01")).thenReturn(java.util.List.of(b01));

        java.util.List<com.annam.dto.DonorDonationDto> donations = donorPortalService.getDonationsForDonor(donor1);

        assertEquals(1, donations.size());
        assertEquals("B01", donations.get(0).getBatchId());
        // Verify repository was strictly invoked with D01, never any other donor
        verify(batchRepo).findByDonorId("D01");
        verify(batchRepo, never()).findByDonorId("D02");
    }

    @Test
    @DisplayName("Scenario 7: Admin approving or rejecting a food donation updates status in PriorityQueue and DB")
    void test11_AdminApproveAndRejectDonation() {
        FoodBatchRepository batchRepo = mock(FoodBatchRepository.class);
        Location loc = new Location("LOC01", "Hub", "Transit");
        Donor d01 = new Donor("D01", "Donor One", "Org One", "123", loc);

        FoodBatch batch = new FoodBatch("B99", FoodCategory.BIRYANI, "Paneer Biryani", 40, 40, 0, 0,
                LocalTime.of(11, 0), LocalTime.of(15, 0), d01, loc, "Fresh", ApprovalStatus.NOT_APPROVED, false, LocalDateTime.now());

        when(batchRepo.findAll()).thenReturn(new java.util.ArrayList<>(java.util.List.of(batch)));
        FoodBatchService batchService = new FoodBatchService(batchRepo, null);

        assertEquals(ApprovalStatus.NOT_APPROVED, batch.getApprovalStatus());
        assertEquals(0, batchService.getApprovedPriorityQueueOrder().size(), "NOT_APPROVED batch must NOT be in approved PriorityQueue");

        // Admin approves batch
        batchService.approveBatch("B99");
        assertEquals(ApprovalStatus.APPROVED, batch.getApprovalStatus());
        verify(batchRepo).updateApproval("B99", ApprovalStatus.APPROVED);
        assertEquals(1, batchService.getApprovedPriorityQueueOrder().size(), "Approved batch MUST be in PriorityQueue");

        // Admin rejects batch with reason
        batchService.rejectBatch("B99", "Expired shelf-life");
        assertEquals(ApprovalStatus.REJECTED, batch.getApprovalStatus());
        assertEquals("Expired shelf-life", batch.getRejectionReason());
        verify(batchRepo).updateRejection("B99", "Expired shelf-life");
        assertEquals(0, batchService.getApprovedPriorityQueueOrder().size(), "Rejected batch MUST be removed from PriorityQueue");
    }

    @Test
    @DisplayName("Scenario 8: Donor viewing updated donation status labels (Pending, Approved, Reserved, Delivered, Rejected, Expired)")
    void test12_DonorDonationStatusMapping() {
        FoodBatchRepository batchRepo = mock(FoodBatchRepository.class);
        FoodBatchService batchService = mock(FoodBatchService.class);
        AllocationRepository allocationRepo = mock(AllocationRepository.class);
        com.annam.service.DonorPortalService donorPortalService = 
                new com.annam.service.DonorPortalService(userRepository, donorRepository, batchRepo, batchService, locationRepository, allocationRepo);

        Location loc = new Location("LOC01", "Hub", "Transit");
        Donor d01 = new Donor("D01", "Donor One", "Org One", "123", loc);

        // 1. Pending Approval
        FoodBatch b1 = new FoodBatch("B01", FoodCategory.MEALS, 20, LocalTime.of(14, 0), d01, loc, ApprovalStatus.NOT_APPROVED);
        assertEquals("Pending Approval", donorPortalService.toDonationDto(b1).getDeliveryStatus());

        // 2. Approved
        FoodBatch b2 = new FoodBatch("B02", FoodCategory.MEALS, 20, LocalTime.of(14, 0), d01, loc, ApprovalStatus.APPROVED);
        assertEquals("Approved", donorPortalService.toDonationDto(b2).getDeliveryStatus());

        // 3. Reserved (all remaining portions reserved)
        FoodBatch b3 = new FoodBatch("B03", FoodCategory.MEALS, 20, 0, 20, 0, LocalTime.of(14, 0), d01, loc, ApprovalStatus.APPROVED, false);
        assertEquals("Reserved", donorPortalService.toDonationDto(b3).getDeliveryStatus());

        // 3b. Partially Allocated (portions partially allocated, remaining available)
        FoodBatch b3b = new FoodBatch("B03b", FoodCategory.MEALS, 20, 10, 10, 0, LocalTime.of(14, 0), d01, loc, ApprovalStatus.APPROVED, false);
        assertEquals("Partially Allocated", donorPortalService.toDonationDto(b3b).getDeliveryStatus());

        // 4. Delivered
        FoodBatch b4 = new FoodBatch("B04", FoodCategory.MEALS, 20, 0, 0, 20, LocalTime.of(14, 0), d01, loc, ApprovalStatus.APPROVED, false);
        assertEquals("Delivered", donorPortalService.toDonationDto(b4).getDeliveryStatus());

        // 5. Rejected
        FoodBatch b5 = new FoodBatch("B05", FoodCategory.MEALS, 20, LocalTime.of(14, 0), d01, loc, ApprovalStatus.REJECTED);
        assertEquals("Rejected", donorPortalService.toDonationDto(b5).getDeliveryStatus());

        // 6. Expired
        FoodBatch b6 = new FoodBatch("B06", FoodCategory.MEALS, 20, 0, 0, 0, LocalTime.of(14, 0), d01, loc, ApprovalStatus.APPROVED, true);
        assertEquals("Expired", donorPortalService.toDonationDto(b6).getDeliveryStatus());
    }

    @Test
    @DisplayName("Scenario 9: Logout followed by subsequent access to protected endpoints is denied with 401")
    void test13_SubsequentAccessAfterLogoutDenied() throws Exception {
        RoleAuthorizationInterceptor interceptor = new RoleAuthorizationInterceptor();
        MockHttpSession session = new MockHttpSession();

        // 1. Logged in as Admin -> Allowed
        User adminUser = new User(1, "admin@annam.org", "hash", UserRole.ADMIN, "Admin", "123", null, "Hub", LocalDateTime.now());
        session.setAttribute(AuthService.SESSION_USER_KEY, adminUser);

        MockHttpServletRequest reqBefore = new MockHttpServletRequest("GET", "/api/dashboard");
        reqBefore.setSession(session);
        MockHttpServletResponse resBefore = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(reqBefore, resBefore, new Object()), "Admin session should access /api/dashboard");

        // 2. User logs out
        authService.logout(session);
        assertTrue(session.isInvalid());

        // 3. Subsequent request without valid session -> 401 Unauthorized
        MockHttpServletRequest reqAfter = new MockHttpServletRequest("GET", "/api/dashboard");
        MockHttpServletResponse resAfter = new MockHttpServletResponse();
        boolean allowedAfter = interceptor.preHandle(reqAfter, resAfter, new Object());

        assertFalse(allowedAfter, "Subsequent access after logout must be denied");
        assertEquals(401, resAfter.getStatus());
    }
}
