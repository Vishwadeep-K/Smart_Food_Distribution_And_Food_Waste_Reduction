package com.annam.repository;

import com.annam.exception.InvalidAllocationException;
import com.annam.graph.Location;
import com.annam.model.Donor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DonorRepository {
    private final JdbcTemplate jdbcTemplate;
    private final LocationRepository locationRepository;

    public DonorRepository(JdbcTemplate jdbcTemplate, LocationRepository locationRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.locationRepository = locationRepository;
    }

    public List<Donor> findAll() {
        return jdbcTemplate.query(
                "SELECT donor_id, name, organization, donor_type, phone, city, location_id FROM donors ORDER BY donor_id",
                (rs, rowNum) -> {
                    Location loc = locationRepository.findById(rs.getString("location_id"));
                    String type = "Restaurant";
                    try { type = rs.getString("donor_type"); } catch (Exception ignored) {}
                    if (type == null || type.isEmpty()) type = "Restaurant";

                    String city = "";
                    try { city = rs.getString("city"); } catch (Exception ignored) {}

                    return new Donor(
                            rs.getString("donor_id"),
                            rs.getString("name"),
                            rs.getString("organization"),
                            type,
                            rs.getString("phone"),
                            city != null ? city : "",
                            loc
                    );
                }
        );
    }

    public Donor findById(String donorId) {
        if (donorId == null) return null;
        List<Donor> list = jdbcTemplate.query(
                "SELECT donor_id, name, organization, donor_type, phone, city, location_id FROM donors WHERE donor_id = ?",
                (rs, rowNum) -> {
                    Location loc = locationRepository.findById(rs.getString("location_id"));
                    String type = "Restaurant";
                    try { type = rs.getString("donor_type"); } catch (Exception ignored) {}
                    if (type == null || type.isEmpty()) type = "Restaurant";

                    String city = "";
                    try { city = rs.getString("city"); } catch (Exception ignored) {}

                    return new Donor(
                            rs.getString("donor_id"),
                            rs.getString("name"),
                            rs.getString("organization"),
                            type,
                            rs.getString("phone"),
                            city != null ? city : "",
                            loc
                    );
                },
                donorId.toUpperCase()
        );
        return list.isEmpty() ? null : list.get(0);
    }

    public void save(Donor donor) {
        jdbcTemplate.update(
                "INSERT INTO donors (donor_id, name, organization, donor_type, phone, city, location_id) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE name = VALUES(name), organization = VALUES(organization), " +
                "donor_type = VALUES(donor_type), phone = VALUES(phone), city = VALUES(city), location_id = VALUES(location_id)",
                donor.getDonorId().toUpperCase(), donor.getName(), donor.getOrganization(),
                donor.getDonorType(), donor.getPhone(), donor.getCity(), donor.getLocation().getLocationId()
        );
    }

    public void delete(String donorId) {
        Integer batchCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM food_batches WHERE donor_id = ?", Integer.class, donorId.toUpperCase());
        if (batchCount != null && batchCount > 0) {
            throw new InvalidAllocationException("Cannot delete donor " + donorId + ": linked to " + batchCount + " existing food batches.");
        }
        jdbcTemplate.update("DELETE FROM donors WHERE donor_id = ?", donorId.toUpperCase());
    }

    public int countBatchesByDonor(String donorId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM food_batches WHERE donor_id = ?", Integer.class, donorId.toUpperCase());
        return count != null ? count : 0;
    }

    public synchronized String getNextDonorId() {
        List<String> ids = jdbcTemplate.query(
                "SELECT donor_id FROM donors WHERE donor_id LIKE 'D%'",
                (rs, rowNum) -> rs.getString("donor_id")
        );
        int maxNum = 0;
        for (String id : ids) {
            try {
                int n = Integer.parseInt(id.substring(1));
                if (n > maxNum) maxNum = n;
            } catch (Exception ignored) {}
        }
        return String.format("D%02d", maxNum + 1);
    }
}
