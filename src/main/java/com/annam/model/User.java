package com.annam.model;

import java.time.LocalDateTime;

/**
 * Represents a registered user account (Admin or Donor) in the system.
 */
public class User {
    private Integer userId;
    private String email;
    private String passwordHash;
    private UserRole role;
    private String fullName;
    private String phone;
    private String donorId; // Null for ADMIN; set to donors(donor_id) for DONOR
    private String pickupAddress;
    private LocalDateTime createdAt;

    public User() {
    }

    public User(Integer userId, String email, String passwordHash, UserRole role,
                String fullName, String phone, String donorId, String pickupAddress, LocalDateTime createdAt) {
        this.userId = userId;
        this.email = email != null ? email.trim().toLowerCase() : null;
        this.passwordHash = passwordHash;
        this.role = role;
        this.fullName = fullName != null ? fullName.trim() : null;
        this.phone = phone != null ? phone.trim() : null;
        this.donorId = donorId != null ? donorId.trim().toUpperCase() : null;
        this.pickupAddress = pickupAddress != null ? pickupAddress.trim() : null;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.trim().toLowerCase() : null;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDonorId() {
        return donorId;
    }

    public void setDonorId(String donorId) {
        this.donorId = donorId != null ? donorId.trim().toUpperCase() : null;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isDonor() {
        return role == UserRole.DONOR;
    }
}
