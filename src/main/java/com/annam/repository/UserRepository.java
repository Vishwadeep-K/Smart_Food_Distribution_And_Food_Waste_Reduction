package com.annam.repository;

import com.annam.model.User;
import com.annam.model.UserRole;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime created = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
        String roleStr = rs.getString("role");
        UserRole role = UserRole.valueOf(roleStr.toUpperCase());

        return new User(
                rs.getInt("user_id"),
                rs.getString("email"),
                rs.getString("password_hash"),
                role,
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getString("donor_id"),
                rs.getString("pickup_address"),
                created
        );
    };

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        try {
            User user = jdbcTemplate.queryForObject(
                    "SELECT user_id, email, password_hash, role, full_name, phone, donor_id, pickup_address, created_at " +
                    "FROM users WHERE LOWER(email) = LOWER(?)",
                    userRowMapper,
                    email.trim()
            );
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findById(int userId) {
        try {
            User user = jdbcTemplate.queryForObject(
                    "SELECT user_id, email, password_hash, role, full_name, phone, donor_id, pickup_address, created_at " +
                    "FROM users WHERE user_id = ?",
                    userRowMapper,
                    userId
            );
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findByDonorId(String donorId) {
        if (donorId == null) return Optional.empty();
        try {
            User user = jdbcTemplate.queryForObject(
                    "SELECT user_id, email, password_hash, role, full_name, phone, donor_id, pickup_address, created_at " +
                    "FROM users WHERE donor_id = ?",
                    userRowMapper,
                    donorId.trim().toUpperCase()
            );
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean existsByEmail(String email) {
        if (email == null) return false;
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(?)",
                Integer.class,
                email.trim()
        );
        return count != null && count > 0;
    }

    public int countByRole(UserRole role) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE role = ?",
                Integer.class,
                role.name()
        );
        return count != null ? count : 0;
    }

    public User save(User user) {
        if (user.getUserId() == null) {
            // INSERT
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO users (email, password_hash, role, full_name, phone, donor_id, pickup_address, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, user.getEmail().toLowerCase());
                ps.setString(2, user.getPasswordHash());
                ps.setString(3, user.getRole().name());
                ps.setString(4, user.getFullName());
                ps.setString(5, user.getPhone());
                ps.setString(6, user.getDonorId());
                ps.setString(7, user.getPickupAddress());
                ps.setTimestamp(8, Timestamp.valueOf(user.getCreatedAt() != null ? user.getCreatedAt() : LocalDateTime.now()));
                return ps;
            }, keyHolder);

            Number key = keyHolder.getKey();
            if (key != null) {
                user.setUserId(key.intValue());
            }
        } else {
            // UPDATE
            jdbcTemplate.update(
                    "UPDATE users SET email = ?, role = ?, full_name = ?, phone = ?, donor_id = ?, pickup_address = ? " +
                    "WHERE user_id = ?",
                    user.getEmail().toLowerCase(),
                    user.getRole().name(),
                    user.getFullName(),
                    user.getPhone(),
                    user.getDonorId(),
                    user.getPickupAddress(),
                    user.getUserId()
            );
        }
        return user;
    }

    public void updateProfile(int userId, String fullName, String phone, String pickupAddress) {
        jdbcTemplate.update(
                "UPDATE users SET full_name = ?, phone = ?, pickup_address = ? WHERE user_id = ?",
                fullName, phone, pickupAddress, userId
        );
    }

    public void updatePassword(int userId, String newPasswordHash) {
        jdbcTemplate.update(
                "UPDATE users SET password_hash = ? WHERE user_id = ?",
                newPasswordHash, userId
        );
    }
}
