package com.annam.model;

public enum FoodCategory {
    MEALS("Standard Cooked Meals"),
    BIRYANI("Biryani & Specialized Hot Rice"),
    RICE("Cooked Rice & Lentils"),
    CHAPATI("Chapati & Flatbreads"),
    BREAD("Bread, Bakery & Baked Goods"),
    FRUITS("Fresh Whole Fruits"),
    VEGETABLES("Fresh Vegetables & Greens"),
    PACKED_FOOD("Packaged Dry Groceries & Shelf Goods");

    private final String displayName;

    FoodCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
