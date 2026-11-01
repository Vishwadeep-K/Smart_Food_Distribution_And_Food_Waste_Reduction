package model;

import graph.Location;
import java.util.Arrays;
import java.util.List;

/**
 * Concrete Recipient representing a community distribution center or neighborhood pantry.
 * Community centres focus on dry goods, fresh produce, packaged foods, and pantry essentials.
 */
public class CommunityCentre extends Recipient {

    public CommunityCentre(String recipientId, String name, Location location, int totalRequiredPortions) {
        super(recipientId, name, location, totalRequiredPortions);
        // Default accepted categories for Community Centres
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
        // Community centres accept pantry items, fresh produce, bread, and packed meals
        return acceptedCategories.contains(category);
    }

    @Override
    public String getRecipientType() {
        return "Community Centre";
    }
}
