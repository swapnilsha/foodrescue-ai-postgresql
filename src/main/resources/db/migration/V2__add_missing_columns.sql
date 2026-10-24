-- ============================================================
-- V2: Add Missing Columns and Tables
-- PostgreSQL Version
-- ============================================================

-- ============================================================
-- Add accepted_by_ngo_id column if it does not exist
-- ============================================================

ALTER TABLE food_donations
    ADD COLUMN IF NOT EXISTS accepted_by_ngo_id BIGINT;

-- ============================================================
-- Add image_path column if it does not exist
-- ============================================================

ALTER TABLE food_donations
    ADD COLUMN IF NOT EXISTS image_path VARCHAR(500);

-- ============================================================
-- Add foreign key for accepted_by_ngo_id if needed
-- ============================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_schema = current_schema()
          AND table_name = 'food_donations'
          AND constraint_name = 'fk_food_accepted_ngo'
    ) THEN

ALTER TABLE food_donations
    ADD CONSTRAINT fk_food_accepted_ngo
        FOREIGN KEY (accepted_by_ngo_id)
            REFERENCES users(id)
            ON DELETE SET NULL;

END IF;
END
$$;

-- ============================================================
-- Add index for accepted_by_ngo_id
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_food_accepted_ngo
    ON food_donations(accepted_by_ngo_id);

-- ============================================================
-- Create notifications table if it does not exist
-- ============================================================

CREATE TABLE IF NOT EXISTS notifications (
                                             id BIGSERIAL PRIMARY KEY,
                                             user_id BIGINT NOT NULL,
                                             message VARCHAR(500) NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    donation_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE,

    FOREIGN KEY (donation_id)
    REFERENCES food_donations(id)
    ON DELETE SET NULL
    );

-- ============================================================
-- Notification indexes
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_notif_user
    ON notifications(user_id);

CREATE INDEX IF NOT EXISTS idx_notif_read
    ON notifications(is_read);