package com.annam.dto;

import java.util.*;

public class DonorReportDto {
    private String reportTitle = "ANNAM Donor Contribution Report";
    private String generatedAt;
    private String dateRangeLabel;
    private String startDate;
    private String endDate;

    // Donor info
    private String donorId;
    private String donorName;
    private String organization;
    private String donorType;
    private String email;
    private String phone;
    private String pickupAddress;
    private String locationName;

    // Summary KPIs
    private int totalDonationsSubmitted;
    private int totalPortionsDonated;
    private int totalPortionsDelivered;
    private int pendingDonationsCount;
    private int approvedDonationsCount;
    private int rejectedDonationsCount;
    private int expiredDonationsCount;
    private double deliverySuccessRate; // e.g. 85.5%

    // Categorized statistics
    private Map<String, Integer> statusSummary = new LinkedHashMap<>();
    private List<CategoryReportItem> categoryBreakdown = new ArrayList<>();
    private List<DateGroupedItem> donationHistoryByDate = new ArrayList<>();
    private List<DonorDonationDto> donations = new ArrayList<>();

    public DonorReportDto() {}

    public String getReportTitle() { return reportTitle; }
    public void setReportTitle(String reportTitle) { this.reportTitle = reportTitle; }
    public String getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }
    public String getDateRangeLabel() { return dateRangeLabel; }
    public void setDateRangeLabel(String dateRangeLabel) { this.dateRangeLabel = dateRangeLabel; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getDonorId() { return donorId; }
    public void setDonorId(String donorId) { this.donorId = donorId; }
    public String getDonorName() { return donorName; }
    public void setDonorName(String donorName) { this.donorName = donorName; }
    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }
    public String getDonorType() { return donorType; }
    public void setDonorType(String donorType) { this.donorType = donorType; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public int getTotalDonationsSubmitted() { return totalDonationsSubmitted; }
    public void setTotalDonationsSubmitted(int totalDonationsSubmitted) { this.totalDonationsSubmitted = totalDonationsSubmitted; }
    public int getTotalPortionsDonated() { return totalPortionsDonated; }
    public void setTotalPortionsDonated(int totalPortionsDonated) { this.totalPortionsDonated = totalPortionsDonated; }
    public int getTotalPortionsDelivered() { return totalPortionsDelivered; }
    public void setTotalPortionsDelivered(int totalPortionsDelivered) { this.totalPortionsDelivered = totalPortionsDelivered; }
    public int getPendingDonationsCount() { return pendingDonationsCount; }
    public void setPendingDonationsCount(int pendingDonationsCount) { this.pendingDonationsCount = pendingDonationsCount; }
    public int getApprovedDonationsCount() { return approvedDonationsCount; }
    public void setApprovedDonationsCount(int approvedDonationsCount) { this.approvedDonationsCount = approvedDonationsCount; }
    public int getRejectedDonationsCount() { return rejectedDonationsCount; }
    public void setRejectedDonationsCount(int rejectedDonationsCount) { this.rejectedDonationsCount = rejectedDonationsCount; }
    public int getExpiredDonationsCount() { return expiredDonationsCount; }
    public void setExpiredDonationsCount(int expiredDonationsCount) { this.expiredDonationsCount = expiredDonationsCount; }
    public double getDeliverySuccessRate() { return deliverySuccessRate; }
    public void setDeliverySuccessRate(double deliverySuccessRate) { this.deliverySuccessRate = deliverySuccessRate; }

    public Map<String, Integer> getStatusSummary() { return statusSummary; }
    public void setStatusSummary(Map<String, Integer> statusSummary) { this.statusSummary = statusSummary; }
    public List<CategoryReportItem> getCategoryBreakdown() { return categoryBreakdown; }
    public void setCategoryBreakdown(List<CategoryReportItem> categoryBreakdown) { this.categoryBreakdown = categoryBreakdown; }
    public List<DateGroupedItem> getDonationHistoryByDate() { return donationHistoryByDate; }
    public void setDonationHistoryByDate(List<DateGroupedItem> donationHistoryByDate) { this.donationHistoryByDate = donationHistoryByDate; }
    public List<DonorDonationDto> getDonations() { return donations; }
    public void setDonations(List<DonorDonationDto> donations) { this.donations = donations; }

    public static class CategoryReportItem {
        private String category;
        private String displayName;
        private int batchCount;
        private int totalPortions;
        private int deliveredPortions;
        private double percentageOfTotal;

        public CategoryReportItem() {}

        public CategoryReportItem(String category, String displayName, int batchCount, int totalPortions, int deliveredPortions, double percentageOfTotal) {
            this.category = category;
            this.displayName = displayName;
            this.batchCount = batchCount;
            this.totalPortions = totalPortions;
            this.deliveredPortions = deliveredPortions;
            this.percentageOfTotal = percentageOfTotal;
        }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public int getBatchCount() { return batchCount; }
        public void setBatchCount(int batchCount) { this.batchCount = batchCount; }
        public int getTotalPortions() { return totalPortions; }
        public void setTotalPortions(int totalPortions) { this.totalPortions = totalPortions; }
        public int getDeliveredPortions() { return deliveredPortions; }
        public void setDeliveredPortions(int deliveredPortions) { this.deliveredPortions = deliveredPortions; }
        public double getPercentageOfTotal() { return percentageOfTotal; }
        public void setPercentageOfTotal(double percentageOfTotal) { this.percentageOfTotal = percentageOfTotal; }
    }

    public static class DateGroupedItem {
        private String date;
        private int count;
        private int portions;
        private int delivered;

        public DateGroupedItem() {}

        public DateGroupedItem(String date, int count, int portions, int delivered) {
            this.date = date;
            this.count = count;
            this.portions = portions;
            this.delivered = delivered;
        }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public int getPortions() { return portions; }
        public void setPortions(int portions) { this.portions = portions; }
        public int getDelivered() { return delivered; }
        public void setDelivered(int delivered) { this.delivered = delivered; }
    }
}
