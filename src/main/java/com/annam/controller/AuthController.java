package com.annam.controller;

import com.annam.dto.LoginRequest;
import com.annam.dto.RegisterRequest;
import com.annam.dto.UserDto;
import com.annam.model.User;
import com.annam.model.UserRole;
import com.annam.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpSession session) {
        try {
            UserDto userDto = authService.authenticate(request, session);
            String redirectUrl = "ADMIN".equalsIgnoreCase(userDto.getRole()) ? "/index.html" : "/donor-dashboard.html";
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Login successful.",
                    "user", userDto,
                    "redirectUrl", redirectUrl
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "An unexpected error occurred during login."
            ));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            UserDto userDto = authService.registerDonor(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Donor registration successful. You can now log in.",
                    "user", userDto
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Registration failed: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        Optional<User> userOpt = authService.getCurrentUser(session);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "authenticated", false,
                    "message", "No active authenticated session."
            ));
        }
        User user = userOpt.get();
        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "user", authService.toDto(user)
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        authService.logout(session);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Successfully logged out.",
                "redirectUrl", "/login.html"
        ));
    }
}
