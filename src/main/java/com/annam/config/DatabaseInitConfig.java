package com.annam.config;

import com.annam.model.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class DatabaseInitConfig implements InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(DatabaseInitConfig.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${annam.admin.email:admin@annam.org}")
    private String adminEmail;

    @Value("${annam.admin.password:Admin@123}")
    private String adminPassword;

    @Value("${annam.admin.name:Annam System Administrator}")
    private String adminName;

    @Value("${annam.donor.sample-email:donor@annam.org}")
    private String sampleDonorEmail;

    @Value("${annam.donor.sample-password:Donor@123}")
    private String sampleDonorPassword;

    public DatabaseInitConfig(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterPropertiesSet() {
        try {
            // 1. Initial base schema check
            Integer tableCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'food_batches'",
                    Integer.class
            );

            if (tableCount == null || tableCount == 0) {
                log.info("Database tables not found. Executing schema.sql and sample_data.sql...");
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                populator.addScript(new ClassPathResource("schema.sql"));
                populator.addScript(new ClassPathResource("sample_data.sql"));
                populator.setContinueOnError(true);
                populator.execute(dataSource);
                log.info("Database initialized successfully with schema and sample data.");
            } else {
                log.info("Database schema already detected with existing tables.");
            }

            // 2. Ensure 'users' table exists (idempotent migration)
            ensureUsersTableExists();

            // 3. Ensure donors table has donor_type and city columns
            ensureDonorsColumnsExist();

            // 4. Ensure food_batches has food_name, preparation_time, notes, and rejection_reason columns
            ensureFoodBatchesColumnsExist();

            // 5. Ensure initial Admin account exists
            ensureAdminAccountExists();

            // 6. Ensure sample Donor account exists (linked to D01)
            ensureSampleDonorAccountExists();

        } catch (Exception e) {
            log.warn("Database initialization check encountered an issue: {}", e.getMessage());
        }
    }

    private void ensureDonorsColumnsExist() {
        try {
            // Check donor_type
            Integer colDonorType = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'donors' AND column_name = 'donor_type'",
                    Integer.class
            );
            if (colDonorType == null || colDonorType == 0) {
                jdbcTemplate.execute("ALTER TABLE donors ADD COLUMN donor_type VARCHAR(32) NULL DEFAULT 'Restaurant' AFTER organization");
                log.info("Added column 'donor_type' to donors table.");
            }

            // Check city
            Integer colCity = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'donors' AND column_name = 'city'",
                    Integer.class
            );
            if (colCity == null || colCity == 0) {
                jdbcTemplate.execute("ALTER TABLE donors ADD COLUMN city VARCHAR(128) NULL AFTER phone");
                log.info("Added column 'city' to donors table.");
            }
        } catch (Exception e) {
            log.warn("Column migration notice on donors: {}", e.getMessage());
        }
    }

    private void ensureUsersTableExists() {
        try {
            Integer userTableCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'users'",
                    Integer.class
            );

            if (userTableCount == null || userTableCount == 0) {
                log.info("Creating 'users' table for Admin and Donor authentication...");
                jdbcTemplate.execute(
                        "CREATE TABLE IF NOT EXISTS users (" +
                        "    user_id INT AUTO_INCREMENT PRIMARY KEY," +
                        "    email VARCHAR(128) NOT NULL UNIQUE," +
                        "    password_hash VARCHAR(255) NOT NULL," +
                        "    role VARCHAR(32) NOT NULL," +
                        "    full_name VARCHAR(128) NOT NULL," +
                        "    phone VARCHAR(32) NULL," +
                        "    donor_id VARCHAR(16) NULL," +
                        "    pickup_address VARCHAR(255) NULL," +
                        "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "    CONSTRAINT fk_user_donor FOREIGN KEY (donor_id) REFERENCES donors(donor_id) ON UPDATE CASCADE ON DELETE SET NULL" +
                        ") ENGINE=InnoDB"
                );
                log.info("'users' table created successfully.");
            }
        } catch (Exception e) {
            log.error("Failed to check or create 'users' table: {}", e.getMessage());
        }
    }

    private void ensureFoodBatchesColumnsExist() {
        try {
            // Check food_name
            Integer colFoodName = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food_batches' AND column_name = 'food_name'",
                    Integer.class
            );
            if (colFoodName == null || colFoodName == 0) {
                jdbcTemplate.execute("ALTER TABLE food_batches ADD COLUMN food_name VARCHAR(128) NULL AFTER food_category");
                log.info("Added column 'food_name' to food_batches table.");
            }

            // Check preparation_time
            Integer colPrepTime = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food_batches' AND column_name = 'preparation_time'",
                    Integer.class
            );
            if (colPrepTime == null || colPrepTime == 0) {
                jdbcTemplate.execute("ALTER TABLE food_batches ADD COLUMN preparation_time TIME NULL AFTER delivered_portions");
                log.info("Added column 'preparation_time' to food_batches table.");
            }

            // Check notes
            Integer colNotes = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food_batches' AND column_name = 'notes'",
                    Integer.class
            );
            if (colNotes == null || colNotes == 0) {
                jdbcTemplate.execute("ALTER TABLE food_batches ADD COLUMN notes TEXT NULL AFTER pickup_location_id");
                log.info("Added column 'notes' to food_batches table.");
            }

            // Check rejection_reason
            Integer colRejection = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food_batches' AND column_name = 'rejection_reason'",
                    Integer.class
            );
            if (colRejection == null || colRejection == 0) {
                jdbcTemplate.execute("ALTER TABLE food_batches ADD COLUMN rejection_reason VARCHAR(255) NULL AFTER notes");
                log.info("Added column 'rejection_reason' to food_batches table.");
            }

            // Check approval_status
            Integer colApproval = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food_batches' AND column_name = 'approval_status'",
                    Integer.class
            );
            if (colApproval == null || colApproval == 0) {
                jdbcTemplate.execute("ALTER TABLE food_batches ADD COLUMN approval_status VARCHAR(24) DEFAULT 'NOT_APPROVED' AFTER rejection_reason");
                log.info("Added column 'approval_status' to food_batches table.");
            }

            // Check is_expired
            Integer colExpired = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food_batches' AND column_name = 'is_expired'",
                    Integer.class
            );
            if (colExpired == null || colExpired == 0) {
                jdbcTemplate.execute("ALTER TABLE food_batches ADD COLUMN is_expired BOOLEAN DEFAULT FALSE AFTER approval_status");
                log.info("Added column 'is_expired' to food_batches table.");
            }

            // Check created_at
            Integer colCreatedAt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food_batches' AND column_name = 'created_at'",
                    Integer.class
            );
            if (colCreatedAt == null || colCreatedAt == 0) {
                jdbcTemplate.execute("ALTER TABLE food_batches ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP AFTER is_expired");
                log.info("Added column 'created_at' to food_batches table.");
            }
        } catch (Exception e) {
            log.warn("Column migration notice on food_batches: {}", e.getMessage());
        }
    }

    private void ensureAdminAccountExists() {
        try {
            Integer adminCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'",
                    Integer.class
            );

            if (adminCount == null || adminCount == 0) {
                String hash = passwordEncoder.encode(adminPassword);
                jdbcTemplate.update(
                        "INSERT INTO users (email, password_hash, role, full_name, phone, donor_id, pickup_address) " +
                        "VALUES (?, ?, 'ADMIN', ?, '+91-98401-00000', NULL, 'ANNAM Central Logistics Hub') " +
                        "ON DUPLICATE KEY UPDATE role = 'ADMIN'",
                        adminEmail.trim().toLowerCase(), hash, adminName
                );
                log.info("Initial Admin account created. Email: [{}], Role: [ADMIN]", adminEmail);
            }
        } catch (Exception e) {
            log.warn("Notice checking Admin account: {}", e.getMessage());
        }
    }

    private void ensureSampleDonorAccountExists() {
        try {
            Integer donorUserCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE email = ?",
                    Integer.class,
                    sampleDonorEmail.trim().toLowerCase()
            );

            if (donorUserCount == null || donorUserCount == 0) {
                // Check if D01 exists
                Integer d01Exists = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM donors WHERE donor_id = 'D01'",
                        Integer.class
                );

                if (d01Exists != null && d01Exists > 0) {
                    String hash = passwordEncoder.encode(sampleDonorPassword);
                    jdbcTemplate.update(
                            "INSERT INTO users (email, password_hash, role, full_name, phone, donor_id, pickup_address) " +
                            "VALUES (?, ?, 'DONOR', 'Grand Palace Hotel', '+91-98401-11001', 'D01', 'Central Hub, Dispatch Road') " +
                            "ON DUPLICATE KEY UPDATE donor_id = 'D01'",
                            sampleDonorEmail.trim().toLowerCase(), hash
                    );
                    log.info("Sample Donor account created. Email: [{}], Role: [DONOR], Donor ID: [D01]", sampleDonorEmail);
                }
            }
        } catch (Exception e) {
            log.warn("Notice checking Sample Donor account: {}", e.getMessage());
        }
    }
}
