package com.annam.dto;

public class DonorProfileDto {
    private String donorId;
    private String name;
    private String organization;
    private String donorType;
    private String email;
    private String phone;
    private String city;
    private String locationId;
    private String locationName;
    private String pickupAddress;
    private int totalDonationsCount;
    private int totalPortionsDonated;
    private int pendingApprovalCount;
    private int deliveredDonationsCount;
    private int deliveredPortionsCount;
    private int expiredOrRejectedCount;

    public DonorProfileDto() {}

    public String getDonorId() {
        return donorId;
    }

    public void setDonorId(String donorId) {
        this.donorId = donorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getDonorType() {
        return donorType;
    }

    public void setDonorType(String donorType) {
        this.donorType = donorType;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public int getTotalDonationsCount() {
        return totalDonationsCount;
    }

    public void setTotalDonationsCount(int totalDonationsCount) {
        this.totalDonationsCount = totalDonationsCount;
    }

    public int getTotalPortionsDonated() {
        return totalPortionsDonated;
    }

    public void setTotalPortionsDonated(int totalPortionsDonated) {
        this.totalPortionsDonated = totalPortionsDonated;
    }

    public int getDeliveredPortionsCount() {
        return deliveredPortionsCount;
    }

    public void setDeliveredPortionsCount(int deliveredPortionsCount) {
        this.deliveredPortionsCount = deliveredPortionsCount;
    }

    public int getPendingApprovalCount() {
        return pendingApprovalCount;
    }

    public void setPendingApprovalCount(int pendingApprovalCount) {
        this.pendingApprovalCount = pendingApprovalCount;
    }

    public int getDeliveredDonationsCount() {
        return deliveredDonationsCount;
    }

    public void setDeliveredDonationsCount(int deliveredDonationsCount) {
        this.deliveredDonationsCount = deliveredDonationsCount;
    }

    public int getExpiredOrRejectedCount() {
        return expiredOrRejectedCount;
    }

    public void setExpiredOrRejectedCount(int expiredOrRejectedCount) {
        this.expiredOrRejectedCount = expiredOrRejectedCount;
    }
}
