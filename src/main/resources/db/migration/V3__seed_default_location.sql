-- ============================================================
-- V3: Seed default location for donation fallback
-- ============================================================

-- Insert a generic default location (user_id=NULL = system default)
-- This prevents "Invalid Location ID" when donors haven't set up a location yet.
INSERT INTO locations (latitude, longitude, address, user_id)
SELECT 18.5204, 73.8567, 'Pune, Maharashtra, India (Default)', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM locations WHERE id = 1
);
