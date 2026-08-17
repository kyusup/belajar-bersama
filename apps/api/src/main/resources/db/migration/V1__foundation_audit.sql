-- Foundation schema only. Educational content tables belong to a later phase.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE audit_event (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL,
    actor_user_id UUID NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NULL,
    target_id UUID NULL,
    correlation_id VARCHAR(64) NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX idx_audit_event_occurred_at ON audit_event (occurred_at DESC);
CREATE INDEX idx_audit_event_actor_user_id ON audit_event (actor_user_id);
CREATE INDEX idx_audit_event_target ON audit_event (target_type, target_id);
CREATE INDEX idx_audit_event_action ON audit_event (action);

COMMENT ON TABLE audit_event IS 'Append-oriented governance/security audit. User and Identity tables are intentionally not created in Phase 2.';
