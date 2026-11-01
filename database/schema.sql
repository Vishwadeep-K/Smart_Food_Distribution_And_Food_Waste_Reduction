-- ============================================================================
-- ANNAM SMART FOOD DISTRIBUTION & FOOD WASTE REDUCTION
-- Database: annam_food_distribution
-- Dialect: MySQL 8.0+ / MariaDB 10.4+ (XAMPP compatible)
-- ============================================================================

CREATE DATABASE IF NOT EXISTS annam_food_distribution
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE annam_food_distribution;

-- 1. Application Settings (Simulated Clock & Configuration)
CREATE TABLE IF NOT EXISTS application_settings (
    setting_key VARCHAR(64) PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL,
    description VARCHAR(255) NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. Locations (Road Network Vertices)
CREATE TABLE IF NOT EXISTS locations (
    location_id VARCHAR(16) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    zone_type VARCHAR(64) NOT NULL,
    description VARCHAR(255) NULL
) ENGINE=InnoDB;

-- 3. Roads (Road Network Edges with Positive Travel Times)
CREATE TABLE IF NOT EXISTS roads (
    road_id INT AUTO_INCREMENT PRIMARY KEY,
    source_location_id VARCHAR(16) NOT NULL,
    destination_location_id VARCHAR(16) NOT NULL,
    travel_time_minutes INT NOT NULL,
    bidirectional BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_road_source FOREIGN KEY (source_location_id) REFERENCES locations(location_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_road_dest FOREIGN KEY (destination_location_id) REFERENCES locations(location_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_travel_time CHECK (travel_time_minutes > 0)
) ENGINE=InnoDB;

-- 4. Donors (Surplus Food Providers)
CREATE TABLE IF NOT EXISTS donors (
    donor_id VARCHAR(16) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    organization VARCHAR(128) NOT NULL,
    donor_type VARCHAR(32) DEFAULT 'Restaurant',
    phone VARCHAR(32) NOT NULL,
    city VARCHAR(128) NULL,
    location_id VARCHAR(16) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_donor_location FOREIGN KEY (location_id) REFERENCES locations(location_id) ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 5. Recipients (Shelters & Community Centres)
CREATE TABLE IF NOT EXISTS recipients (
    recipient_id VARCHAR(16) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    recipient_type VARCHAR(32) NOT NULL, -- 'Shelter' or 'Community Centre'
    location_id VARCHAR(16) NOT NULL,
    total_required_portions INT NOT NULL,
    remaining_demand_portions INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recipient_location FOREIGN KEY (location_id) REFERENCES locations(location_id) ON UPDATE CASCADE,
    CONSTRAINT chk_demand_non_negative CHECK (remaining_demand_portions >= 0)
) ENGINE=InnoDB;

-- 6. Recipient Accepted Food Categories (Polymorphic Filtering)
CREATE TABLE IF NOT EXISTS recipient_accepted_categories (
    recipient_id VARCHAR(16) NOT NULL,
    food_category VARCHAR(32) NOT NULL,
    PRIMARY KEY (recipient_id, food_category),
    CONSTRAINT fk_rac_recipient FOREIGN KEY (recipient_id) REFERENCES recipients(recipient_id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- 7. Delivery Vehicles
CREATE TABLE IF NOT EXISTS vehicles (
    vehicle_id VARCHAR(16) PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    current_location_id VARCHAR(16) NOT NULL,
    portion_capacity INT NOT NULL,
    is_available BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_vehicle_location FOREIGN KEY (current_location_id) REFERENCES locations(location_id) ON UPDATE CASCADE,
    CONSTRAINT chk_capacity_positive CHECK (portion_capacity > 0)
) ENGINE=InnoDB;

-- 8. Food Batches (Surplus Portions with Declared Use-By Time)
CREATE TABLE IF NOT EXISTS food_batches (
    batch_id VARCHAR(16) PRIMARY KEY,
    donor_id VARCHAR(16) NOT NULL,
    food_category VARCHAR(32) NOT NULL,
    food_name VARCHAR(128) NULL,
    total_portions INT NOT NULL,
    available_portions INT NOT NULL,
    reserved_portions INT DEFAULT 0,
    delivered_portions INT DEFAULT 0,
    preparation_time TIME NULL,
    declared_use_by_time TIME NOT NULL,
    pickup_location_id VARCHAR(16) NOT NULL,
    notes TEXT NULL,
    rejection_reason VARCHAR(255) NULL,
    approval_status VARCHAR(24) DEFAULT 'NOT_APPROVED', -- 'APPROVED', 'NOT_APPROVED', 'REJECTED'
    is_expired BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_batch_donor FOREIGN KEY (donor_id) REFERENCES donors(donor_id) ON UPDATE CASCADE,
    CONSTRAINT fk_batch_pickup FOREIGN KEY (pickup_location_id) REFERENCES locations(location_id) ON UPDATE CASCADE,
    CONSTRAINT chk_avail_portions CHECK (available_portions >= 0),
    CONSTRAINT chk_res_portions CHECK (reserved_portions >= 0),
    CONSTRAINT chk_del_portions CHECK (delivered_portions >= 0)
) ENGINE=InnoDB;

-- 9. Food Allocations (Transaction and Assignment Lifecycle)
CREATE TABLE IF NOT EXISTS food_allocations (
    allocation_id VARCHAR(24) PRIMARY KEY,
    batch_id VARCHAR(16) NOT NULL,
    recipient_id VARCHAR(16) NOT NULL,
    vehicle_id VARCHAR(16) NOT NULL,
    allocated_portions INT NOT NULL,
    pickup_location_id VARCHAR(16) NOT NULL,
    destination_location_id VARCHAR(16) NOT NULL,
    travel_time_minutes INT NOT NULL,
    pickup_time TIME NOT NULL,
    handover_minutes INT NOT NULL,
    expected_completion_time TIME NOT NULL,
    use_by_deadline TIME NOT NULL,
    status VARCHAR(24) DEFAULT 'PROPOSED', -- 'PROPOSED', 'RESERVED', 'DELIVERED', 'CANCELLED'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP NULL,
    CONSTRAINT fk_alloc_batch FOREIGN KEY (batch_id) REFERENCES food_batches(batch_id) ON UPDATE CASCADE,
    CONSTRAINT fk_alloc_recipient FOREIGN KEY (recipient_id) REFERENCES recipients(recipient_id) ON UPDATE CASCADE,
    CONSTRAINT fk_alloc_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id) ON UPDATE CASCADE,
    CONSTRAINT fk_alloc_pickup FOREIGN KEY (pickup_location_id) REFERENCES locations(location_id) ON UPDATE CASCADE,
    CONSTRAINT fk_alloc_dest FOREIGN KEY (destination_location_id) REFERENCES locations(location_id) ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 10. Route Locations for Allocations (Stores Dijkstra Steps)
CREATE TABLE IF NOT EXISTS allocation_route_locations (
    allocation_id VARCHAR(24) NOT NULL,
    step_order INT NOT NULL,
    location_id VARCHAR(16) NOT NULL,
    PRIMARY KEY (allocation_id, step_order),
    CONSTRAINT fk_route_alloc FOREIGN KEY (allocation_id) REFERENCES food_allocations(allocation_id) ON DELETE CASCADE,
    CONSTRAINT fk_route_loc FOREIGN KEY (location_id) REFERENCES locations(location_id) ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 11. Allocation Audit History
CREATE TABLE IF NOT EXISTS allocation_history (
    history_id INT AUTO_INCREMENT PRIMARY KEY,
    allocation_id VARCHAR(24) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    event_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    details TEXT NULL,
    CONSTRAINT fk_history_alloc FOREIGN KEY (allocation_id) REFERENCES food_allocations(allocation_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Indexes for fast query performance
CREATE INDEX idx_batches_category ON food_batches(food_category);
CREATE INDEX idx_batches_approval ON food_batches(approval_status);
CREATE INDEX idx_batches_use_by ON food_batches(declared_use_by_time);
CREATE INDEX idx_alloc_status ON food_allocations(status);

-- 12. System Users (Authentication & Role-Based Access: ADMIN, DONOR)
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(128) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL, -- 'ADMIN', 'DONOR'
    full_name VARCHAR(128) NOT NULL,
    phone VARCHAR(32) NULL,
    donor_id VARCHAR(16) NULL,
    pickup_address VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_donor FOREIGN KEY (donor_id) REFERENCES donors(donor_id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_donor_id ON users(donor_id);
