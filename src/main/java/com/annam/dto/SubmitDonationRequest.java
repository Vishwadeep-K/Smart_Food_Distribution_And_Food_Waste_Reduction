package com.annam.dto;

public class SubmitDonationRequest {
    private String foodName;
    private String category;
    private Integer totalPortions;
    private String preparationTime; // HH:mm
    private String useByTime;         // HH:mm
    private String pickupLocationId;
    private String notes;

    public SubmitDonationRequest() {}

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getTotalPortions() {
        return totalPortions;
    }

    public void setTotalPortions(Integer totalPortions) {
        this.totalPortions = totalPortions;
    }

    public String getPreparationTime() {
        return preparationTime;
    }

    public void setPreparationTime(String preparationTime) {
        this.preparationTime = preparationTime;
    }

    public String getUseByTime() {
        return useByTime;
    }

    public void setUseByTime(String useByTime) {
        this.useByTime = useByTime;
    }

    public String getPickupLocationId() {
        return pickupLocationId;
    }

    public void setPickupLocationId(String pickupLocationId) {
        this.pickupLocationId = pickupLocationId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
