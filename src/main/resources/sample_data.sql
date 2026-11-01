-- ============================================================================
-- ANNAM SMART FOOD DISTRIBUTION & FOOD WASTE REDUCTION
-- Synthetic Sample Data
-- Scope: Exactly 5 Donors, Exactly 4 Recipients, Exactly 3 Vehicles,
--        12 Locations, 22 Food Batches, Road Network Connections
-- ============================================================================

USE annam_food_distribution;

-- 1. Initial Application Settings
INSERT INTO application_settings (setting_key, setting_value, description) VALUES
('simulated_time', '10:00:00', 'Current simulated time for distribution clock'),
('handover_minutes', '10', 'Fixed safety handover allowance for recipient unloading')
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);

-- 2. Road Network Locations (12 Locations)
INSERT INTO locations (location_id, name, zone_type, description) VALUES
('LOC01', 'Central Hub', 'Transit & Dispatch', 'Central transit terminal and main dispatch yard'),
('LOC02', 'South Campus', 'Educational Zone', 'University campus canteen and student union food hall'),
('LOC03', 'West Market', 'Commercial Center', 'Wholesale food hub and bakery boulevard'),
('LOC04', 'North Gate', 'Dining Corridor', 'Restaurant precinct and banquet hall cluster'),
('LOC05', 'Tech Park', 'Corporate Complex', 'Software park cafeteria and corporate food court'),
('LOC06', 'East Ward', 'Residential & Shelter', 'Shelter cluster and children home zone'),
('LOC07', 'Riverside', 'Care Home Enclave', 'Elder care living quarters and assisted home'),
('LOC08', 'Old Town', 'Community Hub', 'Heritage community hall and welfare centre'),
('LOC09', 'Metro Cross', 'Suburban Center', 'Suburban food pantry and welfare distribution centre'),
('LOC10', 'Junction Alpha', 'Way Point', 'Major road junction linking North Gate and East Ward'),
('LOC11', 'Junction Beta', 'Way Point', 'High-capacity ring road crossing linking West Market & Old Town'),
('LOC12', 'Express Ring', 'Highway Connector', 'Express highway corridor connecting Tech Park & Metro Cross')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 3. Road Connections (Positive Travel Times in Minutes)
INSERT INTO roads (source_location_id, destination_location_id, travel_time_minutes, bidirectional) VALUES
('LOC01', 'LOC10', 8, TRUE),
('LOC10', 'LOC04', 10, TRUE),
('LOC04', 'LOC12', 12, TRUE),
('LOC12', 'LOC05', 15, TRUE),
('LOC01', 'LOC03', 14, TRUE),
('LOC03', 'LOC11', 9, TRUE),
('LOC11', 'LOC02', 11, TRUE),
('LOC01', 'LOC06', 18, TRUE),
('LOC06', 'LOC07', 15, TRUE),
('LOC07', 'LOC08', 16, TRUE),
('LOC08', 'LOC09', 12, TRUE),
('LOC10', 'LOC06', 10, TRUE),
('LOC11', 'LOC08', 14, TRUE),
('LOC05', 'LOC07', 20, TRUE),
('LOC02', 'LOC09', 22, TRUE),
('LOC12', 'LOC09', 25, TRUE);

-- 4. Exactly 5 Donors
INSERT INTO donors (donor_id, name, organization, phone, location_id) VALUES
('D01', 'Grand Palace Hotel', 'Grand Palace Hospitality Ltd', '+91-98401-11001', 'LOC01'),
('D02', 'Anna Univ Canteen', 'Anna University Food Services', '+91-98401-11002', 'LOC02'),
('D03', 'Green Leaf Bakery', 'Green Leaf Artisans', '+91-98401-11003', 'LOC03'),
('D04', 'Spice Garden Rest', 'Spice Garden Dining Group', '+91-98401-11004', 'LOC04'),
('D05', 'Tech Park Cafeteria', 'Global Tech Food Commons', '+91-98401-11005', 'LOC05')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 5. Exactly 4 Recipients (2 Shelters, 2 Community Centres)
INSERT INTO recipients (recipient_id, name, recipient_type, location_id, total_required_portions, remaining_demand_portions) VALUES
('R01', 'Karuna Children Home', 'Shelter', 'LOC06', 60, 60),
('R02', 'Mother Teresa Senior Home', 'Shelter', 'LOC07', 40, 40),
('R03', 'Hope Community Centre', 'Community Centre', 'LOC08', 50, 50),
('R04', 'Peace Welfare Pantry', 'Community Centre', 'LOC09', 35, 35)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 6. Recipient Accepted Food Categories
-- Shelters accept cooked and ready-to-serve food: MEALS, BIRYANI, RICE, CHAPATI, BREAD, PACKED_FOOD
INSERT INTO recipient_accepted_categories (recipient_id, food_category) VALUES
('R01', 'MEALS'),
('R01', 'BIRYANI'),
('R01', 'RICE'),
('R01', 'CHAPATI'),
('R01', 'BREAD'),
('R01', 'PACKED_FOOD'),

('R02', 'MEALS'),
('R02', 'BIRYANI'),
('R02', 'RICE'),
('R02', 'CHAPATI'),
('R02', 'BREAD'),
('R02', 'PACKED_FOOD'),

-- Community Centres accept fresh produce, pantry essentials, and packaged foods: FRUITS, VEGETABLES, PACKED_FOOD, BREAD, MEALS
('R03', 'FRUITS'),
('R03', 'VEGETABLES'),
('R03', 'PACKED_FOOD'),
('R03', 'BREAD'),
('R03', 'MEALS'),

('R04', 'FRUITS'),
('R04', 'VEGETABLES'),
('R04', 'PACKED_FOOD'),
('R04', 'BREAD'),
('R04', 'MEALS')
ON DUPLICATE KEY UPDATE food_category = VALUES(food_category);

-- 7. Exactly 3 Delivery Vehicles
INSERT INTO vehicles (vehicle_id, name, current_location_id, portion_capacity, is_available) VALUES
('V01', 'Annam Van-01 (Heavy)', 'LOC01', 50, TRUE),
('V02', 'Annam Van-02 (Medium)', 'LOC03', 30, TRUE),
('V03', 'Annam E-Loader-03 (Compact)', 'LOC02', 20, TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 8. At Least 20 Food Batches (22 Distinct Batches)
-- Format: BatchID, Donor, Category, Total, Available, Reserved, Delivered, Use-By, Pickup, Approval, Expired
INSERT INTO food_batches (batch_id, donor_id, food_category, total_portions, available_portions, reserved_portions, delivered_portions, declared_use_by_time, pickup_location_id, approval_status, is_expired) VALUES
('B01', 'D01', 'MEALS', 40, 40, 0, 0, '12:30:00', 'LOC01', 'APPROVED', FALSE),
('B02', 'D04', 'BIRYANI', 30, 30, 0, 0, '11:30:00', 'LOC04', 'APPROVED', FALSE),
('B03', 'D02', 'RICE', 25, 25, 0, 0, '13:00:00', 'LOC02', 'APPROVED', FALSE),
('B04', 'D03', 'BREAD', 50, 50, 0, 0, '15:30:00', 'LOC03', 'APPROVED', FALSE),
('B05', 'D05', 'FRUITS', 35, 35, 0, 0, '16:00:00', 'LOC05', 'APPROVED', FALSE),
('B06', 'D05', 'VEGETABLES', 45, 45, 0, 0, '14:30:00', 'LOC05', 'APPROVED', FALSE),
('B07', 'D01', 'CHAPATI', 30, 30, 0, 0, '12:00:00', 'LOC01', 'APPROVED', FALSE),
('B08', 'D03', 'PACKED_FOOD', 60, 60, 0, 0, '17:00:00', 'LOC03', 'APPROVED', FALSE),
-- B09: Expired past 10:20 when clock advances to 10:30 (for testing)
('B09', 'D04', 'MEALS', 20, 20, 0, 0, '10:20:00', 'LOC04', 'APPROVED', FALSE),
-- B10: Tight deadline (10:45) for testing travel deadline feasibility rejection
('B10', 'D01', 'BIRYANI', 35, 35, 0, 0, '10:45:00', 'LOC01', 'APPROVED', FALSE),
-- B11 & B12: Unapproved batches for testing approval enforcement
('B11', 'D02', 'RICE', 40, 40, 0, 0, '13:30:00', 'LOC02', 'NOT_APPROVED', FALSE),
('B12', 'D04', 'CHAPATI', 25, 25, 0, 0, '14:00:00', 'LOC04', 'NOT_APPROVED', FALSE),
-- B13 - B22: Additional approved batches
('B13', 'D03', 'BREAD', 30, 30, 0, 0, '15:00:00', 'LOC03', 'APPROVED', FALSE),
-- B14 shares EXACT same use-by time (13:00:00) as B03 for deterministic tie-breaker verification
('B14', 'D05', 'MEALS', 50, 50, 0, 0, '13:00:00', 'LOC05', 'APPROVED', FALSE),
('B15', 'D01', 'PACKED_FOOD', 40, 40, 0, 0, '16:30:00', 'LOC01', 'APPROVED', FALSE),
('B16', 'D02', 'VEGETABLES', 20, 20, 0, 0, '13:45:00', 'LOC02', 'APPROVED', FALSE),
('B17', 'D03', 'FRUITS', 25, 25, 0, 0, '14:15:00', 'LOC03', 'APPROVED', FALSE),
('B18', 'D04', 'BIRYANI', 15, 15, 0, 0, '12:15:00', 'LOC04', 'APPROVED', FALSE),
('B19', 'D05', 'RICE', 30, 30, 0, 0, '14:45:00', 'LOC05', 'APPROVED', FALSE),
('B20', 'D01', 'CHAPATI', 40, 40, 0, 0, '13:15:00', 'LOC01', 'APPROVED', FALSE),
('B21', 'D02', 'MEALS', 35, 35, 0, 0, '15:15:00', 'LOC02', 'APPROVED', FALSE),
('B22', 'D03', 'PACKED_FOOD', 50, 50, 0, 0, '18:00:00', 'LOC03', 'APPROVED', FALSE)
ON DUPLICATE KEY UPDATE food_category = VALUES(food_category);

-- 9. Initial System Users (Admin and Sample Donors)
-- Passwords: Admin -> Admin@123, Donor -> Donor@123 (hashed with BCrypt)
INSERT INTO users (email, password_hash, role, full_name, phone, donor_id, pickup_address) VALUES
('admin@annam.org', '$2a$10$wKdXK1eutwBA8CfAL//PnOgpkivu9Wo0p6KkS7llyTY.6L0k9zzzK', 'ADMIN', 'Annam System Administrator', '+91-98401-00000', NULL, 'ANNAM Central Logistics Hub'),
('donor@annam.org', '$2a$10$/npSMYJdUXgdr6PTAjVJ3OXOa5eMfuXR1yPPhb7zk/bZOURnHUyF6', 'DONOR', 'Grand Palace Hotel', '+91-98401-11001', 'D01', 'Central Hub, Dispatch Road'),
('annacanteen@annam.org', '$2a$10$/npSMYJdUXgdr6PTAjVJ3OXOa5eMfuXR1yPPhb7zk/bZOURnHUyF6', 'DONOR', 'Anna Univ Canteen', '+91-98401-11002', 'D02', 'South Campus Food Hall'),
('greenleaf@annam.org', '$2a$10$/npSMYJdUXgdr6PTAjVJ3OXOa5eMfuXR1yPPhb7zk/bZOURnHUyF6', 'DONOR', 'Green Leaf Bakery', '+91-98401-11003', 'D03', 'West Market Boulevard')
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

