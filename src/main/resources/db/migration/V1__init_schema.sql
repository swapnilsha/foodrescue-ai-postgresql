-- ============================================================
-- V1: Core Schema Initialization
-- PostgreSQL Version
-- ============================================================

-- Core Users Table
CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY,
                                     email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Locations
CREATE TABLE IF NOT EXISTS locations (
                                         id BIGSERIAL PRIMARY KEY,
                                         latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    address TEXT NOT NULL,
    user_id BIGINT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- Food Donations
CREATE TABLE IF NOT EXISTS food_donations (
                                              id BIGSERIAL PRIMARY KEY,
                                              donor_id BIGINT NOT NULL,
                                              accepted_by_ngo_id BIGINT,
                                              title VARCHAR(255) NOT NULL,
    food_category VARCHAR(100) NOT NULL,
    quantity_kg DECIMAL(5, 2) NOT NULL,
    status VARCHAR(50) DEFAULT 'AVAILABLE',
    expiry_time TIMESTAMP NOT NULL,
    location_id BIGINT NOT NULL,
    image_path VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (donor_id) REFERENCES users(id),
    FOREIGN KEY (accepted_by_ngo_id) REFERENCES users(id),
    FOREIGN KEY (location_id) REFERENCES locations(id)
    );

-- AI Analysis Results
CREATE TABLE IF NOT EXISTS food_analysis (
                                             id BIGSERIAL PRIMARY KEY,
                                             donation_id BIGINT NOT NULL,
                                             confidence_score DECIMAL(5, 2),
    freshness_score DECIMAL(5, 2),
    estimated_quantity_kg DECIMAL(5, 2),
    predicted_expiry TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (donation_id) REFERENCES food_donations(id) ON DELETE CASCADE
    );

-- Notifications
CREATE TABLE IF NOT EXISTS notifications (
                                             id BIGSERIAL PRIMARY KEY,
                                             user_id BIGINT NOT NULL,
                                             message VARCHAR(500) NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    donation_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (donation_id) REFERENCES food_donations(id) ON DELETE SET NULL
    );

-- Indexes
CREATE INDEX idx_food_status ON food_donations(status);
CREATE INDEX idx_food_expiry ON food_donations(expiry_time);
CREATE INDEX idx_food_donor ON food_donations(donor_id);
CREATE INDEX idx_notif_user ON notifications(user_id);
CREATE INDEX idx_notif_read ON notifications(is_read);