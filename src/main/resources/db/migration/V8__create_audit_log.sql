CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(64) NOT NULL,
    entity_id BIGINT NULL,
    action VARCHAR(32) NOT NULL,
    actor_user_id BIGINT NULL,
    details VARCHAR(1000) NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_audit_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_user ON audit_log (actor_user_id);
