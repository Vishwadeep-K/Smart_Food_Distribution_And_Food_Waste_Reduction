package com.annam.repository;

import com.annam.graph.Location;
import com.annam.graph.PathResult;
import com.annam.model.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AllocationRepository {
    private final JdbcTemplate jdbcTemplate;
    private final FoodBatchRepository batchRepository;
    private final RecipientRepository recipientRepository;
    private final VehicleRepository vehicleRepository;
    private final LocationRepository locationRepository;

    public AllocationRepository(JdbcTemplate jdbcTemplate, FoodBatchRepository batchRepository,
                                RecipientRepository recipientRepository, VehicleRepository vehicleRepository,
                                LocationRepository locationRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.batchRepository = batchRepository;
        this.recipientRepository = recipientRepository;
        this.vehicleRepository = vehicleRepository;
        this.locationRepository = locationRepository;
    }

    public List<FoodAllocation> findAll() {
        return jdbcTemplate.query(
                "SELECT allocation_id, batch_id, recipient_id, vehicle_id, allocated_portions, " +
                "pickup_location_id, destination_location_id, travel_time_minutes, pickup_time, " +
                "handover_minutes, status FROM food_allocations ORDER BY created_at DESC",
                (rs, rowNum) -> {
                    String id = rs.getString("allocation_id");
                    FoodBatch batch = batchRepository.findById(rs.getString("batch_id"));
                    Recipient recipient = recipientRepository.findById(rs.getString("recipient_id"));
                    DeliveryVehicle vehicle = vehicleRepository.findById(rs.getString("vehicle_id"));
                    int qty = rs.getInt("allocated_portions");
                    Location pickup = locationRepository.findById(rs.getString("pickup_location_id"));
                    Location dest = locationRepository.findById(rs.getString("destination_location_id"));
                    int travelTime = rs.getInt("travel_time_minutes");
                    Time pt = rs.getTime("pickup_time");
                    LocalTime pickupTime = pt != null ? pt.toLocalTime() : LocalTime.of(10, 0);
                    int handover = rs.getInt("handover_minutes");
                    AllocationStatus status = AllocationStatus.valueOf(rs.getString("status"));

                    List<Location> path = findRouteLocations(id);
                    if (path.isEmpty()) {
                        path = List.of(pickup, dest);
                    }
                    PathResult pathResult = PathResult.success(pickup, dest, path, travelTime);

                    return new FoodAllocation(id, batch, recipient, vehicle, qty, pathResult, pickupTime, handover, status);
                }
        );
    }

    public FoodAllocation findById(String allocationId) {
        if (allocationId == null) return null;
        List<FoodAllocation> list = jdbcTemplate.query(
                "SELECT allocation_id, batch_id, recipient_id, vehicle_id, allocated_portions, " +
                "pickup_location_id, destination_location_id, travel_time_minutes, pickup_time, " +
                "handover_minutes, status FROM food_allocations WHERE allocation_id = ?",
                (rs, rowNum) -> {
                    String id = rs.getString("allocation_id");
                    FoodBatch batch = batchRepository.findById(rs.getString("batch_id"));
                    Recipient recipient = recipientRepository.findById(rs.getString("recipient_id"));
                    DeliveryVehicle vehicle = vehicleRepository.findById(rs.getString("vehicle_id"));
                    int qty = rs.getInt("allocated_portions");
                    Location pickup = locationRepository.findById(rs.getString("pickup_location_id"));
                    Location dest = locationRepository.findById(rs.getString("destination_location_id"));
                    int travelTime = rs.getInt("travel_time_minutes");
                    Time pt = rs.getTime("pickup_time");
                    LocalTime pickupTime = pt != null ? pt.toLocalTime() : LocalTime.of(10, 0);
                    int handover = rs.getInt("handover_minutes");
                    AllocationStatus status = AllocationStatus.valueOf(rs.getString("status"));

                    List<Location> path = findRouteLocations(id);
                    if (path.isEmpty()) {
                        path = List.of(pickup, dest);
                    }
                    PathResult pathResult = PathResult.success(pickup, dest, path, travelTime);

                    return new FoodAllocation(id, batch, recipient, vehicle, qty, pathResult, pickupTime, handover, status);
                },
                allocationId.toUpperCase()
        );
        return list.isEmpty() ? null : list.get(0);
    }

    public List<FoodAllocation> findByBatchId(String batchId) {
        if (batchId == null) return java.util.Collections.emptyList();
        return jdbcTemplate.query(
                "SELECT allocation_id, batch_id, recipient_id, vehicle_id, allocated_portions, " +
                "pickup_location_id, destination_location_id, travel_time_minutes, pickup_time, " +
                "handover_minutes, status FROM food_allocations WHERE batch_id = ? ORDER BY created_at ASC",
                (rs, rowNum) -> {
                    String id = rs.getString("allocation_id");
                    FoodBatch batch = batchRepository.findById(rs.getString("batch_id"));
                    Recipient recipient = recipientRepository.findById(rs.getString("recipient_id"));
                    DeliveryVehicle vehicle = vehicleRepository.findById(rs.getString("vehicle_id"));
                    int qty = rs.getInt("allocated_portions");
                    Location pickup = locationRepository.findById(rs.getString("pickup_location_id"));
                    Location dest = locationRepository.findById(rs.getString("destination_location_id"));
                    int travelTime = rs.getInt("travel_time_minutes");
                    Time pt = rs.getTime("pickup_time");
                    LocalTime pickupTime = pt != null ? pt.toLocalTime() : LocalTime.of(10, 0);
                    int handover = rs.getInt("handover_minutes");
                    AllocationStatus status = AllocationStatus.valueOf(rs.getString("status"));

                    List<Location> path = findRouteLocations(id);
                    if (path.isEmpty()) {
                        path = List.of(pickup, dest);
                    }
                    PathResult pathResult = PathResult.success(pickup, dest, path, travelTime);

                    return new FoodAllocation(id, batch, recipient, vehicle, qty, pathResult, pickupTime, handover, status);
                },
                batchId.toUpperCase()
        );
    }

    public void save(FoodAllocation alloc) {
        jdbcTemplate.update(
                "INSERT INTO food_allocations (allocation_id, batch_id, recipient_id, vehicle_id, allocated_portions, " +
                "pickup_location_id, destination_location_id, travel_time_minutes, pickup_time, handover_minutes, " +
                "expected_completion_time, use_by_deadline, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE status = VALUES(status)",
                alloc.getAllocationId(), alloc.getFoodBatch().getBatchId(), alloc.getRecipient().getRecipientId(),
                alloc.getVehicle().getVehicleId(), alloc.getAllocatedPortions(), alloc.getPickupLocation().getLocationId(),
                alloc.getDestinationLocation().getLocationId(), alloc.getTravelTimeMinutes(),
                Time.valueOf(alloc.getPickupTime()), alloc.getHandoverMinutes(),
                Time.valueOf(alloc.getExpectedCompletionTime()),
                Time.valueOf(alloc.getFoodBatch().getDeclaredUseByTime()), alloc.getStatus().name()
        );

        saveRouteSteps(alloc.getAllocationId(), alloc.getRouteResult().getPath());
    }

    public void updateStatus(String allocationId, AllocationStatus status) {
        if (status == AllocationStatus.DELIVERED) {
            jdbcTemplate.update(
                    "UPDATE food_allocations SET status = ?, confirmed_at = CURRENT_TIMESTAMP WHERE allocation_id = ?",
                    status.name(), allocationId.toUpperCase()
            );
        } else {
            jdbcTemplate.update(
                    "UPDATE food_allocations SET status = ? WHERE allocation_id = ?",
                    status.name(), allocationId.toUpperCase()
            );
        }
    }

    private void saveRouteSteps(String allocationId, List<Location> path) {
        if (path == null) return;
        jdbcTemplate.update("DELETE FROM allocation_route_locations WHERE allocation_id = ?", allocationId);
        int step = 1;
        for (Location loc : path) {
            jdbcTemplate.update(
                    "INSERT INTO allocation_route_locations (allocation_id, step_order, location_id) VALUES (?, ?, ?)",
                    allocationId, step++, loc.getLocationId()
            );
        }
    }

    public List<Location> findRouteLocations(String allocationId) {
        return jdbcTemplate.query(
                "SELECT l.location_id, l.name, l.zone_type, l.description FROM allocation_route_locations arl " +
                "JOIN locations l ON arl.location_id = l.location_id " +
                "WHERE arl.allocation_id = ? ORDER BY arl.step_order ASC",
                (rs, rowNum) -> new Location(
                        rs.getString("location_id"),
                        rs.getString("name"),
                        rs.getString("zone_type"),
                        rs.getString("description")
                ),
                allocationId.toUpperCase()
        );
    }

    public void recordHistory(String allocationId, String eventType, String details) {
        jdbcTemplate.update(
                "INSERT INTO allocation_history (allocation_id, event_type, details) VALUES (?, ?, ?)",
                allocationId.toUpperCase(), eventType, details
        );
    }

    public List<HistoryRecordDto> findAllHistory() {
        return jdbcTemplate.query(
                "SELECT ah.history_id, ah.allocation_id, ah.event_type, ah.event_timestamp, ah.details, " +
                "fa.batch_id, fa.recipient_id, fa.vehicle_id, fa.allocated_portions " +
                "FROM allocation_history ah " +
                "JOIN food_allocations fa ON ah.allocation_id = fa.allocation_id " +
                "ORDER BY ah.event_timestamp DESC",
                (rs, rowNum) -> new HistoryRecordDto(
                        rs.getInt("history_id"),
                        rs.getString("allocation_id"),
                        rs.getString("event_type"),
                        rs.getTimestamp("event_timestamp"),
                        rs.getString("details"),
                        rs.getString("batch_id"),
                        rs.getString("recipient_id"),
                        rs.getString("vehicle_id"),
                        rs.getInt("allocated_portions")
                )
        );
    }

    public record HistoryRecordDto(int historyId, String allocationId, String eventType,
                                   Timestamp timestamp, String details, String batchId,
                                   String recipientId, String vehicleId, int allocatedPortions) {}
}
