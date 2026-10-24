-- ============================================================
-- V4: Add Donation Request & Delivery Workflow Table
-- PostgreSQL Version
-- ============================================================

CREATE TABLE IF NOT EXISTS donation_requests (
                                                 id                  BIGSERIAL PRIMARY KEY,
                                                 donation_id         BIGINT          NOT NULL,
                                                 donor_id            BIGINT          NOT NULL,
                                                 ngo_id              BIGINT          NOT NULL,

    -- Workflow status
                                                 status              VARCHAR(30)     NOT NULL DEFAULT 'PENDING',

    -- Timestamps for each workflow stage
    requested_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at         TIMESTAMP       NULL,
    rejected_at         TIMESTAMP       NULL,
    rejection_reason    VARCHAR(500)    NULL,

    assigned_at         TIMESTAMP       NULL,
    assigned_to_name    VARCHAR(255)    NULL,
    assigned_to_phone   VARCHAR(30)     NULL,

    pickup_started_at   TIMESTAMP       NULL,
    collected_at        TIMESTAMP       NULL,
    received_at         TIMESTAMP       NULL,
    delivered_at        TIMESTAMP       NULL,

    received_quantity_kg DECIMAL(5,2)   NULL,
    delivery_notes      VARCHAR(1000)   NULL,

    -- Optimistic locking version
    version             BIGINT          NOT NULL DEFAULT 0,

    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_dr_donation
    FOREIGN KEY (donation_id)
    REFERENCES food_donations(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_dr_donor
    FOREIGN KEY (donor_id)
    REFERENCES users(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_dr_ngo
    FOREIGN KEY (ngo_id)
    REFERENCES users(id)
    ON DELETE CASCADE
    );

-- Indexes
CREATE INDEX IF NOT EXISTS idx_dr_donation
    ON donation_requests(donation_id);

CREATE INDEX IF NOT EXISTS idx_dr_donor
    ON donation_requests(donor_id);

CREATE INDEX IF NOT EXISTS idx_dr_ngo
    ON donation_requests(ngo_id);

CREATE INDEX IF NOT EXISTS idx_dr_status
    ON donation_requests(status);

CREATE INDEX IF NOT EXISTS idx_dr_requested
    ON donation_requests(requested_at);