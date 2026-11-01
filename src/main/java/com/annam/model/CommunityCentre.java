package com.annam.model;

import com.annam.graph.Location;
import java.util.Arrays;
import java.util.List;

/**
 * Concrete Recipient representing a community welfare centre or pantry.
 * Community centres focus on dry goods, fresh produce, packaged foods, and groceries.
 */
public class CommunityCentre extends Recipient {

    public CommunityCentre(String recipientId, String name, Location location, int totalRequiredPortions) {
        super(recipientId, name, location, totalRequiredPortions);
        acceptedCategories.addAll(Arrays.asList(
                FoodCategory.FRUITS,
                FoodCategory.VEGETABLES,
                FoodCategory.PACKED_FOOD,
                FoodCategory.BREAD,
                FoodCategory.MEALS
        ));
    }

    public CommunityCentre(String recipientId, String name, Location location, int totalRequiredPortions, List<FoodCategory> customCategories) {
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
        return "Community Centre";
    }
}
