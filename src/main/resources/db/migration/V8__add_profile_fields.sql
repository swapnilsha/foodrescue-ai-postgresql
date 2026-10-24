-- ============================================================
-- V8: Add profile-specific columns to users table
-- PostgreSQL Version
-- ============================================================

-- Organization name
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS organization_name VARCHAR(255) DEFAULT NULL;

-- Legal registration ID
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS legal_registration_id VARCHAR(100) DEFAULT NULL;