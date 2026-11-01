package com.annam.repository;

import com.annam.graph.Location;
import com.annam.model.CommunityCentre;
import com.annam.model.FoodCategory;
import com.annam.model.Recipient;
import com.annam.model.Shelter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public class RecipientRepository {
    private final JdbcTemplate jdbcTemplate;
    private final LocationRepository locationRepository;

    public RecipientRepository(JdbcTemplate jdbcTemplate, LocationRepository locationRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.locationRepository = locationRepository;
    }

    public List<Recipient> findAll() {
        return jdbcTemplate.query(
                "SELECT recipient_id, name, recipient_type, location_id, total_required_portions, remaining_demand_portions FROM recipients ORDER BY recipient_id",
                (rs, rowNum) -> {
                    String id = rs.getString("recipient_id");
                    String name = rs.getString("name");
                    String type = rs.getString("recipient_type");
                    Location loc = locationRepository.findById(rs.getString("location_id"));
                    int total = rs.getInt("total_required_portions");
                    int remaining = rs.getInt("remaining_demand_portions");

                    Recipient recipient = createRecipientInstance(id, name, type, loc, total);
                    recipient.setRemainingDemandPortions(remaining);
                    loadAcceptedCategories(recipient);
                    return recipient;
                }
        );
    }

    public Recipient findById(String recipientId) {
        if (recipientId == null) return null;
        List<Recipient> list = jdbcTemplate.query(
                "SELECT recipient_id, name, recipient_type, location_id, total_required_portions, remaining_demand_portions FROM recipients WHERE recipient_id = ?",
                (rs, rowNum) -> {
                    String id = rs.getString("recipient_id");
                    String name = rs.getString("name");
                    String type = rs.getString("recipient_type");
                    Location loc = locationRepository.findById(rs.getString("location_id"));
                    int total = rs.getInt("total_required_portions");
                    int remaining = rs.getInt("remaining_demand_portions");

                    Recipient recipient = createRecipientInstance(id, name, type, loc, total);
                    recipient.setRemainingDemandPortions(remaining);
                    loadAcceptedCategories(recipient);
                    return recipient;
                },
                recipientId.toUpperCase()
        );
        return list.isEmpty() ? null : list.get(0);
    }

    private Recipient createRecipientInstance(String id, String name, String type, Location loc, int total) {
        if ("Community Centre".equalsIgnoreCase(type) || "CommunityCentre".equalsIgnoreCase(type)) {
            return new CommunityCentre(id, name, loc, total);
        } else {
            return new Shelter(id, name, loc, total);
        }
    }

    private void loadAcceptedCategories(Recipient recipient) {
        List<String> cats = jdbcTemplate.queryForList(
                "SELECT food_category FROM recipient_accepted_categories WHERE recipient_id = ?",
                String.class, recipient.getRecipientId().toUpperCase()
        );
        Set<FoodCategory> set = new HashSet<>();
        for (String c : cats) {
            try {
                set.add(FoodCategory.valueOf(c.toUpperCase()));
            } catch (Exception ignored) {}
        }
        recipient.setAcceptedCategories(set);
    }

    @Transactional
    public void save(Recipient recipient) {
        jdbcTemplate.update(
                "INSERT INTO recipients (recipient_id, name, recipient_type, location_id, total_required_portions, remaining_demand_portions) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE name = VALUES(name), recipient_type = VALUES(recipient_type), " +
                "location_id = VALUES(location_id), total_required_portions = VALUES(total_required_portions), " +
                "remaining_demand_portions = VALUES(remaining_demand_portions)",
                recipient.getRecipientId().toUpperCase(), recipient.getName(), recipient.getRecipientType(),
                recipient.getLocation().getLocationId(), recipient.getTotalRequiredPortions(), recipient.getRemainingDemandPortions()
        );

        jdbcTemplate.update("DELETE FROM recipient_accepted_categories WHERE recipient_id = ?", recipient.getRecipientId().toUpperCase());
        for (FoodCategory cat : recipient.getAcceptedCategories()) {
            jdbcTemplate.update(
                    "INSERT INTO recipient_accepted_categories (recipient_id, food_category) VALUES (?, ?)",
                    recipient.getRecipientId().toUpperCase(), cat.name()
            );
        }
    }

    public void updateDemand(String recipientId, int remainingDemand) {
        jdbcTemplate.update(
                "UPDATE recipients SET remaining_demand_portions = ? WHERE recipient_id = ?",
                Math.max(0, remainingDemand), recipientId.toUpperCase()
        );
    }
}
