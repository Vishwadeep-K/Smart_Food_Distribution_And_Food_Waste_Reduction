package com.annam.repository;

import com.annam.graph.Location;
import com.annam.graph.RoadNetwork;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LocationRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Location> locationRowMapper = (rs, rowNum) -> new Location(
            rs.getString("location_id"),
            rs.getString("name"),
            rs.getString("zone_type"),
            rs.getString("description")
    );

    public LocationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Location> findAllLocations() {
        return jdbcTemplate.query("SELECT location_id, name, zone_type, description FROM locations ORDER BY location_id", locationRowMapper);
    }

    public Location findById(String locationId) {
        List<Location> list = jdbcTemplate.query(
                "SELECT location_id, name, zone_type, description FROM locations WHERE location_id = ?",
                locationRowMapper, locationId.toUpperCase());
        return list.isEmpty() ? null : list.get(0);
    }

    public void save(Location loc) {
        jdbcTemplate.update(
                "INSERT INTO locations (location_id, name, zone_type, description) VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE name = VALUES(name), zone_type = VALUES(zone_type), description = VALUES(description)",
                loc.getLocationId(), loc.getName(), loc.getZoneType(), loc.getDescription()
        );
    }

    public RoadNetwork loadRoadNetwork() {
        RoadNetwork network = new RoadNetwork();
        List<Location> locs = findAllLocations();
        for (Location loc : locs) {
            network.addLocation(loc);
        }

        jdbcTemplate.query(
                "SELECT source_location_id, destination_location_id, travel_time_minutes, bidirectional FROM roads",
                rs -> {
                    String src = rs.getString("source_location_id");
                    String dst = rs.getString("destination_location_id");
                    int time = rs.getInt("travel_time_minutes");
                    boolean bidi = rs.getBoolean("bidirectional");
                    if (bidi) {
                        network.addBidirectionalRoad(src, dst, time);
                    } else {
                        network.addDirectedRoad(src, dst, time);
                    }
                }
        );

        return network;
    }

    public List<RoadDto> findAllRoads() {
        return jdbcTemplate.query(
                "SELECT r.road_id, r.source_location_id, l1.name as source_name, " +
                "r.destination_location_id, l2.name as destination_name, r.travel_time_minutes, r.bidirectional " +
                "FROM roads r " +
                "JOIN locations l1 ON r.source_location_id = l1.location_id " +
                "JOIN locations l2 ON r.destination_location_id = l2.location_id " +
                "ORDER BY r.road_id",
                (rs, rowNum) -> new RoadDto(
                        rs.getInt("road_id"),
                        rs.getString("source_location_id"),
                        rs.getString("source_name"),
                        rs.getString("destination_location_id"),
                        rs.getString("destination_name"),
                        rs.getInt("travel_time_minutes"),
                        rs.getBoolean("bidirectional")
                )
        );
    }

    public record RoadDto(int roadId, String sourceId, String sourceName, String destId, String destName, int travelTimeMinutes, boolean bidirectional) {}
}
