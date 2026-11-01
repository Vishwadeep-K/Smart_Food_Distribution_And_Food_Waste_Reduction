package com.annam.repository;

import com.annam.graph.Location;
import com.annam.model.DeliveryVehicle;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class VehicleRepository {
    private final JdbcTemplate jdbcTemplate;
    private final LocationRepository locationRepository;

    public VehicleRepository(JdbcTemplate jdbcTemplate, LocationRepository locationRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.locationRepository = locationRepository;
    }

    public List<DeliveryVehicle> findAll() {
        return jdbcTemplate.query(
                "SELECT vehicle_id, name, current_location_id, portion_capacity, is_available FROM vehicles ORDER BY vehicle_id",
                (rs, rowNum) -> {
                    Location loc = locationRepository.findById(rs.getString("current_location_id"));
                    return new DeliveryVehicle(
                            rs.getString("vehicle_id"),
                            rs.getString("name"),
                            loc,
                            rs.getInt("portion_capacity"),
                            rs.getBoolean("is_available")
                    );
                }
        );
    }

    public DeliveryVehicle findById(String vehicleId) {
        if (vehicleId == null) return null;
        List<DeliveryVehicle> list = jdbcTemplate.query(
                "SELECT vehicle_id, name, current_location_id, portion_capacity, is_available FROM vehicles WHERE vehicle_id = ?",
                (rs, rowNum) -> {
                    Location loc = locationRepository.findById(rs.getString("current_location_id"));
                    return new DeliveryVehicle(
                            rs.getString("vehicle_id"),
                            rs.getString("name"),
                            loc,
                            rs.getInt("portion_capacity"),
                            rs.getBoolean("is_available")
                    );
                },
                vehicleId.toUpperCase()
        );
        return list.isEmpty() ? null : list.get(0);
    }

    public void save(DeliveryVehicle vehicle) {
        jdbcTemplate.update(
                "INSERT INTO vehicles (vehicle_id, name, current_location_id, portion_capacity, is_available) VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE name = VALUES(name), current_location_id = VALUES(current_location_id), " +
                "portion_capacity = VALUES(portion_capacity), is_available = VALUES(is_available)",
                vehicle.getVehicleId().toUpperCase(), vehicle.getName(), vehicle.getCurrentLocation().getLocationId(),
                vehicle.getPortionCapacity(), vehicle.isAvailable()
        );
    }

    public void updateLocationAndAvailability(String vehicleId, String locationId, boolean isAvailable) {
        jdbcTemplate.update(
                "UPDATE vehicles SET current_location_id = ?, is_available = ? WHERE vehicle_id = ?",
                locationId.toUpperCase(), isAvailable, vehicleId.toUpperCase()
        );
    }

    public void setAvailable(String vehicleId, boolean isAvailable) {
        jdbcTemplate.update(
                "UPDATE vehicles SET is_available = ? WHERE vehicle_id = ?",
                isAvailable, vehicleId.toUpperCase()
        );
    }
}
