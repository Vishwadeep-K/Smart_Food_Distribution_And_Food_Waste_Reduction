package com.annam.repository;

import com.annam.graph.Location;
import com.annam.model.ApprovalStatus;
import com.annam.model.Donor;
import com.annam.model.FoodBatch;
import com.annam.model.FoodCategory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Time;
import java.time.LocalTime;
import java.util.List;

@Repository
public class FoodBatchRepository {
    private final JdbcTemplate jdbcTemplate;
    private final DonorRepository donorRepository;
    private final LocationRepository locationRepository;

    private final RowMapper<FoodBatch> batchRowMapper;

    public FoodBatchRepository(JdbcTemplate jdbcTemplate, DonorRepository donorRepository, LocationRepository locationRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.donorRepository = donorRepository;
        this.locationRepository = locationRepository;
        this.batchRowMapper = (rs, rowNum) -> {
            String id = rs.getString("batch_id");
            Donor donor = donorRepository.findById(rs.getString("donor_id"));
            FoodCategory cat = FoodCategory.valueOf(rs.getString("food_category").toUpperCase());
            int total = rs.getInt("total_portions");
            int avail = rs.getInt("available_portions");
            int res = rs.getInt("reserved_portions");
            int del = rs.getInt("delivered_portions");
            Time t = rs.getTime("declared_use_by_time");
            LocalTime useBy = t != null ? t.toLocalTime() : LocalTime.of(12, 0);
            Location loc = locationRepository.findById(rs.getString("pickup_location_id"));
            ApprovalStatus appr = ApprovalStatus.valueOf(rs.getString("approval_status").toUpperCase());
            boolean exp = rs.getBoolean("is_expired");

            String foodName = null;
            try { foodName = rs.getString("food_name"); } catch (Exception ignored) {}

            LocalTime prepTime = null;
            try {
                Time pt = rs.getTime("preparation_time");
                if (pt != null) prepTime = pt.toLocalTime();
            } catch (Exception ignored) {}

            String notes = null;
            try { notes = rs.getString("notes"); } catch (Exception ignored) {}

            String rejectionReason = null;
            try { rejectionReason = rs.getString("rejection_reason"); } catch (Exception ignored) {}

            java.time.LocalDateTime createdAt = null;
            try {
                java.sql.Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) createdAt = ts.toLocalDateTime();
            } catch (Exception ignored) {}

            return new FoodBatch(id, cat, foodName, total, avail, res, del, prepTime, useBy, donor, loc, notes, rejectionReason, appr, exp, createdAt);
        };
    }

    public List<FoodBatch> findAll() {
        return jdbcTemplate.query(
                "SELECT batch_id, donor_id, food_category, food_name, total_portions, available_portions, " +
                "reserved_portions, delivered_portions, preparation_time, declared_use_by_time, pickup_location_id, " +
                "notes, rejection_reason, approval_status, is_expired, created_at FROM food_batches ORDER BY declared_use_by_time ASC, batch_id ASC",
                batchRowMapper
        );
    }

    public FoodBatch findById(String batchId) {
        if (batchId == null) return null;
        List<FoodBatch> list = jdbcTemplate.query(
                "SELECT batch_id, donor_id, food_category, food_name, total_portions, available_portions, " +
                "reserved_portions, delivered_portions, preparation_time, declared_use_by_time, pickup_location_id, " +
                "notes, rejection_reason, approval_status, is_expired, created_at FROM food_batches WHERE batch_id = ?",
                batchRowMapper,
                batchId.toUpperCase()
        );
        return list.isEmpty() ? null : list.get(0);
    }

    public List<FoodBatch> findApprovedBatches() {
        return jdbcTemplate.query(
                "SELECT batch_id, donor_id, food_category, food_name, total_portions, available_portions, " +
                "reserved_portions, delivered_portions, preparation_time, declared_use_by_time, pickup_location_id, " +
                "notes, rejection_reason, approval_status, is_expired, created_at FROM food_batches " +
                "WHERE approval_status = 'APPROVED' AND is_expired = FALSE AND available_portions > 0 " +
                "ORDER BY declared_use_by_time ASC, batch_id ASC",
                batchRowMapper
        );
    }

    public List<FoodBatch> findByDonorId(String donorId) {
        if (donorId == null) return java.util.Collections.emptyList();
        return jdbcTemplate.query(
                "SELECT batch_id, donor_id, food_category, food_name, total_portions, available_portions, " +
                "reserved_portions, delivered_portions, preparation_time, declared_use_by_time, pickup_location_id, " +
                "notes, rejection_reason, approval_status, is_expired, created_at FROM food_batches " +
                "WHERE donor_id = ? ORDER BY created_at DESC, batch_id DESC",
                batchRowMapper,
                donorId.toUpperCase()
        );
    }

    public synchronized String getNextBatchId() {
        List<String> ids = jdbcTemplate.query(
                "SELECT batch_id FROM food_batches WHERE batch_id LIKE 'B%'",
                (rs, rowNum) -> rs.getString("batch_id")
        );
        int maxNum = 0;
        for (String id : ids) {
            try {
                int n = Integer.parseInt(id.substring(1));
                if (n > maxNum) maxNum = n;
            } catch (Exception ignored) {}
        }
        return String.format("B%02d", maxNum + 1);
    }

    public void save(FoodBatch batch) {
        jdbcTemplate.update(
                "INSERT INTO food_batches (batch_id, donor_id, food_category, food_name, total_portions, available_portions, " +
                "reserved_portions, delivered_portions, preparation_time, declared_use_by_time, pickup_location_id, " +
                "notes, rejection_reason, approval_status, is_expired) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE donor_id = VALUES(donor_id), food_category = VALUES(food_category), " +
                "food_name = VALUES(food_name), total_portions = VALUES(total_portions), " +
                "available_portions = VALUES(available_portions), reserved_portions = VALUES(reserved_portions), " +
                "delivered_portions = VALUES(delivered_portions), preparation_time = VALUES(preparation_time), " +
                "declared_use_by_time = VALUES(declared_use_by_time), pickup_location_id = VALUES(pickup_location_id), " +
                "notes = VALUES(notes), rejection_reason = VALUES(rejection_reason), approval_status = VALUES(approval_status), is_expired = VALUES(is_expired)",
                batch.getBatchId().toUpperCase(), batch.getDonor().getDonorId(), batch.getCategory().name(),
                batch.getFoodName(), batch.getTotalPortions(), batch.getAvailablePortions(), batch.getReservedPortions(),
                batch.getDeliveredPortions(),
                batch.getPreparationTime() != null ? Time.valueOf(batch.getPreparationTime()) : null,
                Time.valueOf(batch.getDeclaredUseByTime()), batch.getPickupLocation().getLocationId(),
                batch.getNotes(), batch.getRejectionReason(), batch.getApprovalStatus().name(), batch.isExpired()
        );
    }

    public boolean reservePortionsConditionally(String batchId, int portions) {
        int updated = jdbcTemplate.update(
                "UPDATE food_batches SET available_portions = available_portions - ?, " +
                "reserved_portions = reserved_portions + ? " +
                "WHERE batch_id = ? AND available_portions >= ? AND approval_status = 'APPROVED' AND is_expired = FALSE",
                portions, portions, batchId.toUpperCase(), portions
        );
        return updated > 0;
    }

    public void releaseReservation(String batchId, int portions) {
        jdbcTemplate.update(
                "UPDATE food_batches SET available_portions = available_portions + ?, " +
                "reserved_portions = GREATEST(0, reserved_portions - ?) " +
                "WHERE batch_id = ?",
                portions, portions, batchId.toUpperCase()
        );
    }

    public void confirmDelivered(String batchId, int portions) {
        jdbcTemplate.update(
                "UPDATE food_batches SET reserved_portions = GREATEST(0, reserved_portions - ?), " +
                "delivered_portions = delivered_portions + ? " +
                "WHERE batch_id = ?",
                portions, portions, batchId.toUpperCase()
        );
    }

    public void updateApproval(String batchId, ApprovalStatus status) {
        jdbcTemplate.update(
                "UPDATE food_batches SET approval_status = ?, rejection_reason = CASE WHEN ? = 'APPROVED' THEN NULL ELSE rejection_reason END WHERE batch_id = ?",
                status.name(), status.name(), batchId.toUpperCase()
        );
    }

    public void updateRejection(String batchId, String reason) {
        jdbcTemplate.update(
                "UPDATE food_batches SET approval_status = 'REJECTED', rejection_reason = ? WHERE batch_id = ?",
                reason, batchId.toUpperCase()
        );
    }

    public void markExpired(String batchId) {
        jdbcTemplate.update(
                "UPDATE food_batches SET is_expired = TRUE, available_portions = 0 WHERE batch_id = ?",
                batchId.toUpperCase()
        );
    }
}
