package model;

/**
 * Categories of surplus food accepted and distributed in the Annam network.
 */
public enum FoodCategory {
    RICE("Rice & Grain Dishes", true),
    MEALS("Cooked Meals & Platters", true),
    BREAD("Bakery & Bread Items", false),
    FRUITS("Fresh Fruits", false),
    VEGETABLES("Fresh Vegetables", false),
    CHAPATI("Chapati & Roti Packs", true),
    BIRYANI("Biryani & Specialized Hot Rice", true),
    PACKED_FOOD("Packaged & Sealed Food", false);

    private final String displayName;
    private final boolean hotCooked;

    FoodCategory(String displayName, boolean hotCooked) {
        this.displayName = displayName;
        this.hotCooked = hotCooked;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isHotCooked() {
        return hotCooked;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
