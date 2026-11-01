package com.annam.service;

import com.annam.dto.LoginRequest;
import com.annam.dto.RegisterRequest;
import com.annam.dto.UserDto;
import com.annam.graph.Location;
import com.annam.model.Donor;
import com.annam.model.User;
import com.annam.model.UserRole;
import com.annam.repository.DonorRepository;
import com.annam.repository.LocationRepository;
import com.annam.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    public static final String SESSION_USER_KEY = "currentUser";

    private final UserRepository userRepository;
    private final DonorRepository donorRepository;
    private final LocationRepository locationRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, DonorRepository donorRepository,
                       LocationRepository locationRepository) {
        this.userRepository = userRepository;
        this.donorRepository = donorRepository;
        this.locationRepository = locationRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    public UserDto authenticate(LoginRequest request, HttpSession session) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Email and password are required.");
        }

        String email = request.getEmail().trim().toLowerCase();
        String password = request.getPassword();
        String requestedRole = request.getRole() != null ? request.getRole().trim().toUpperCase() : null;

        if (email.isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("Email and password cannot be empty.");
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        // Strict role validation
        if (requestedRole != null) {
            if ("ADMIN".equals(requestedRole) && user.getRole() != UserRole.ADMIN) {
                throw new IllegalArgumentException("Access Denied: This account is registered as a Donor. Please switch to Donor Login.");
            }
            if ("DONOR".equals(requestedRole) && user.getRole() != UserRole.DONOR) {
                throw new IllegalArgumentException("Access Denied: This account has Admin privileges. Please switch to Admin Login.");
            }
        }

        // Store authenticated user in session
        session.setAttribute(SESSION_USER_KEY, user);

        return toDto(user);
    }

    @Transactional
    public UserDto registerDonor(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration details cannot be empty.");
        }

        String fullName = request.getFullName() != null ? request.getFullName().trim() : "";
        String orgName = request.getOrganization() != null ? request.getOrganization().trim() : "";
        String donorType = request.getDonorType() != null && !request.getDonorType().trim().isEmpty() 
                ? request.getDonorType().trim() : "Restaurant";
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        String phone = request.getPhone() != null ? request.getPhone().trim() : "";
        String city = request.getCity() != null ? request.getCity().trim() : "";
        String pickupAddress = request.getPickupAddress() != null ? request.getPickupAddress().trim() : "";
        String password = request.getPassword();
        String confirmPassword = request.getConfirmPassword();
        String locationId = request.getLocationId() != null ? request.getLocationId().trim().toUpperCase() : "LOC01";

        // Required Validations
        if (fullName.isEmpty()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("A valid email address is required.");
        }
        if (phone.isEmpty()) {
            throw new IllegalArgumentException("Mobile phone number is required.");
        }
        if (pickupAddress.isEmpty()) {
            throw new IllegalArgumentException("Pickup address is required.");
        }
        if (city.isEmpty()) {
            throw new IllegalArgumentException("City or locality is required.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Password and confirm password do not match.");
        }

        // Duplicate email check
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("The email address '" + email + "' is already registered. Please log in or use another email.");
        }

        Location loc = locationRepository.findById(locationId);
        if (loc == null) {
            loc = locationRepository.findById("LOC01");
        }

        // 1. Create Donor record
        String donorId = donorRepository.getNextDonorId();
        String finalOrg = !orgName.isEmpty() ? orgName : fullName;
        Donor donor = new Donor(donorId, fullName, finalOrg, donorType, phone, city, loc);
        donorRepository.save(donor);

        // 2. Hash password & Create User record strictly with DONOR role
        String hash = passwordEncoder.encode(password);
        User user = new User(
                null,
                email,
                hash,
                UserRole.DONOR, // Strict: Registrations are ALWAYS DONOR. No privilege escalation.
                fullName,
                phone,
                donorId,
                pickupAddress,
                LocalDateTime.now()
        );
        user = userRepository.save(user);

        return toDto(user);
    }

    public Optional<User> getCurrentUser(HttpSession session) {
        if (session == null) return Optional.empty();
        Object obj = session.getAttribute(SESSION_USER_KEY);
        if (obj instanceof User user) {
            return userRepository.findById(user.getUserId());
        }
        return Optional.empty();
    }

    public void logout(HttpSession session) {
        if (session != null) {
            session.removeAttribute(SESSION_USER_KEY);
            session.invalidate();
        }
    }

    public UserDto toDto(User user) {
        return new UserDto(
                user.getUserId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole().name(),
                user.getDonorId(),
                user.getPickupAddress()
        );
    }
}
