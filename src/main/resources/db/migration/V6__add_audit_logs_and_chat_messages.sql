-- ============================================================
-- V6: Add Audit Logs and Chat Messages Tables
-- PostgreSQL Version
-- ============================================================

CREATE TABLE IF NOT EXISTS audit_logs (
                                          id          BIGSERIAL PRIMARY KEY,
                                          actor       VARCHAR(255) NOT NULL,
    action      VARCHAR(255) NOT NULL,
    severity    VARCHAR(50)  NOT NULL,
    description TEXT         NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS chat_messages (
                                             id              BIGSERIAL PRIMARY KEY,
                                             sender_email    VARCHAR(255) NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    content         TEXT         NOT NULL,
    room_id         VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_audit_created
    ON audit_logs(created_at);

CREATE INDEX IF NOT EXISTS idx_chat_room
    ON chat_messages(room_id);