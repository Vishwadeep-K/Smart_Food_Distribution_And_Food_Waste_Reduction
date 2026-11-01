package com.annam.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Repository
public class SettingsRepository {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final JdbcTemplate jdbcTemplate;

    public SettingsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public LocalTime getSimulatedTime() {
        try {
            String val = jdbcTemplate.queryForObject(
                    "SELECT setting_value FROM application_settings WHERE setting_key = 'simulated_time'",
                    String.class);
            if (val != null) {
                if (val.length() == 5) val = val + ":00";
                return LocalTime.parse(val, TIME_FMT);
            }
        } catch (EmptyResultDataAccessException ignored) {}
        return LocalTime.of(10, 0);
    }

    public void setSimulatedTime(LocalTime time) {
        String val = time.format(TIME_FMT);
        jdbcTemplate.update(
                "INSERT INTO application_settings (setting_key, setting_value, description) VALUES ('simulated_time', ?, 'Current simulated time') " +
                "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)",
                val);
    }

    public int getHandoverMinutes() {
        try {
            String val = jdbcTemplate.queryForObject(
                    "SELECT setting_value FROM application_settings WHERE setting_key = 'handover_minutes'",
                    String.class);
            if (val != null) {
                return Integer.parseInt(val.trim());
            }
        } catch (Exception ignored) {}
        return 10;
    }

    public void setHandoverMinutes(int minutes) {
        jdbcTemplate.update(
                "INSERT INTO application_settings (setting_key, setting_value, description) VALUES ('handover_minutes', ?, 'Safety handover allowance in minutes') " +
                "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)",
                String.valueOf(minutes));
    }
}
