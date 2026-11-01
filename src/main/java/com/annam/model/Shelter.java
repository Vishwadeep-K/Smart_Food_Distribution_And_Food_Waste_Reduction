package com.annam.model;

import com.annam.graph.Location;
import java.util.Arrays;
import java.util.List;

/**
 * Concrete Recipient representing a shelter (homeless shelter, orphanage, elder care home).
 * Shelters prioritize hot cooked meals and ready-to-eat provisions for immediate consumption.
 */
public class Shelter extends Recipient {

    public Shelter(String recipientId, String name, Location location, int totalRequiredPortions) {
        super(recipientId, name, location, totalRequiredPortions);
        acceptedCategories.addAll(Arrays.asList(
                FoodCategory.MEALS,
                FoodCategory.BIRYANI,
                FoodCategory.RICE,
                FoodCategory.CHAPATI,
                FoodCategory.BREAD,
                FoodCategory.PACKED_FOOD
        ));
    }

    public Shelter(String recipientId, String name, Location location, int totalRequiredPortions, List<FoodCategory> customCategories) {
        super(recipientId, name, location, totalRequiredPortions);
        if (customCategories != null) {
            acceptedCategories.addAll(customCategories);
        }
    }

    @Override
    public boolean acceptsFoodCategory(FoodCategory category) {
        if (category == null) return false;
        return acceptedCategories.contains(category);
    }

    @Override
    public String getRecipientType() {
        return "Shelter";
    }
}
