-- ============================================================
-- V5: Fix food_donations.status and add fields
-- PostgreSQL Version
-- ============================================================

-- V1 has already been converted to VARCHAR(30),
-- so no ENUM conversion is required here.

-- ============================================================
-- Add urgency column
-- ============================================================

ALTER TABLE food_donations
    ADD COLUMN IF NOT EXISTS urgency VARCHAR(10) DEFAULT 'HIGH';

-- ============================================================
-- Add notes column
-- ============================================================

ALTER TABLE food_donations
    ADD COLUMN IF NOT EXISTS notes TEXT;