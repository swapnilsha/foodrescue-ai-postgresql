-- ============================================================
-- V7: Create System Settings Table and default configuration
-- ============================================================

CREATE TABLE IF NOT EXISTS system_settings (
    setting_key   VARCHAR(255) PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL
);

INSERT INTO system_settings (setting_key, setting_value) VALUES ('maintenance_mode', 'false');
INSERT INTO system_settings (setting_key, setting_value) VALUES ('debug_logging', 'true');
INSERT INTO system_settings (setting_key, setting_value) VALUES ('max_search_radius', '50');
INSERT INTO system_settings (setting_key, setting_value) VALUES ('urgent_pickup_threshold', '4');
