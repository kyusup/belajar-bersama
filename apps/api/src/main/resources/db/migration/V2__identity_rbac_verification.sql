-- Identity, RBAC, competencies, verification. Educational content tables remain later.
CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    display_name VARCHAR(120) NOT NULL,
    avatar_url TEXT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT app_user_status_chk CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DEACTIVATED'))
);

CREATE TABLE identity_link (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user (id),
    provider VARCHAR(32) NOT NULL,
    issuer VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT identity_link_provider_chk CHECK (provider IN ('GOOGLE', 'APPLE')),
    CONSTRAINT identity_link_unique UNIQUE (provider, issuer, subject)
);

CREATE INDEX idx_identity_link_user_id ON identity_link (user_id);

CREATE TABLE auth_session (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user (id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NULL
);

CREATE INDEX idx_auth_session_user_id ON auth_session (user_id);
CREATE INDEX idx_auth_session_expires_at ON auth_session (expires_at);

CREATE TABLE oauth_state (
    state VARCHAR(80) PRIMARY KEY,
    provider VARCHAR(32) NOT NULL,
    code_verifier VARCHAR(128) NOT NULL,
    nonce VARCHAR(80) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE app_role (
    id VARCHAR(64) PRIMARY KEY
);

CREATE TABLE app_permission (
    id VARCHAR(64) PRIMARY KEY
);

CREATE TABLE role_permission (
    role_id VARCHAR(64) NOT NULL REFERENCES app_role (id),
    permission_id VARCHAR(64) NOT NULL REFERENCES app_permission (id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_role (
    user_id UUID NOT NULL REFERENCES app_user (id),
    role_id VARCHAR(64) NOT NULL REFERENCES app_role (id),
    assigned_by UUID NULL REFERENCES app_user (id),
    assigned_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE competency (
    id UUID PRIMARY KEY,
    slug VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE verification (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user (id),
    competency_id UUID NOT NULL REFERENCES competency (id),
    status VARCHAR(32) NOT NULL,
    qualification TEXT NULL,
    experience TEXT NULL,
    reviewer_id UUID NULL REFERENCES app_user (id),
    decision_note TEXT NULL,
    decided_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT verification_status_chk CHECK (
        status IN (
            'DRAFT',
            'SUBMITTED',
            'UNDER_REVIEW',
            'CHANGES_REQUESTED',
            'APPROVED',
            'REJECTED',
            'REVOKED'
        )
    )
);

CREATE INDEX idx_verification_user_id ON verification (user_id);
CREATE INDEX idx_verification_status ON verification (status);
CREATE UNIQUE INDEX uq_verification_approved_scope
    ON verification (user_id, competency_id)
    WHERE status = 'APPROVED';
CREATE UNIQUE INDEX uq_verification_open_scope
    ON verification (user_id, competency_id)
    WHERE status IN ('DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'CHANGES_REQUESTED');

CREATE TABLE verification_evidence (
    id UUID PRIMARY KEY,
    verification_id UUID NOT NULL REFERENCES verification (id) ON DELETE CASCADE,
    kind VARCHAR(64) NOT NULL,
    summary TEXT NOT NULL,
    reference_url TEXT NULL,
    storage_key TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_verification_evidence_verification_id ON verification_evidence (verification_id);

INSERT INTO app_role (id) VALUES
    ('LEARNER'),
    ('VERIFIED_CONTRIBUTOR'),
    ('CHECKER'),
    ('MODERATOR'),
    ('ADMINISTRATOR');

INSERT INTO app_permission (id) VALUES
    ('USER_READ_SELF'),
    ('CONTENT_READ_PUBLISHED'),
    ('LEARNING_PROGRESS_MANAGE'),
    ('BOOKMARK_MANAGE'),
    ('QUIZ_HISTORY_READ'),
    ('QA_CREATE'),
    ('QA_ASK'),
    ('QA_ANSWER'),
    ('QA_MARK_USEFUL'),
    ('QA_ACCEPT_ANSWER'),
    ('CONTENT_REPORT'),
    ('CONTENT_CREATE'),
    ('CONTENT_EDIT_OWN'),
    ('CONTENT_UPDATE_DRAFT'),
    ('CONTENT_SUBMIT'),
    ('CONTENT_REVIEW'),
    ('CONTENT_REQUEST_CHANGES'),
    ('CONTENT_APPROVE'),
    ('CONTENT_PUBLISH'),
    ('CONTENT_ARCHIVE'),
    ('CONTENT_MODERATE'),
    ('CONTENT_REPORT_REVIEW'),
    ('VERIFICATION_APPLY'),
    ('VERIFICATION_REVIEW'),
    ('VERIFICATION_APPROVE'),
    ('VERIFICATION_REVOKE'),
    ('VERIFICATION_GRANT'),
    ('TAXONOMY_MANAGE'),
    ('USER_MANAGE'),
    ('ROLE_MANAGE'),
    ('ROLE_ASSIGN'),
    ('SYSTEM_ADMIN'),
    ('AUDIT_READ');

INSERT INTO role_permission (role_id, permission_id) VALUES
    ('LEARNER', 'USER_READ_SELF'),
    ('LEARNER', 'CONTENT_READ_PUBLISHED'),
    ('LEARNER', 'LEARNING_PROGRESS_MANAGE'),
    ('LEARNER', 'BOOKMARK_MANAGE'),
    ('LEARNER', 'QUIZ_HISTORY_READ'),
    ('LEARNER', 'QA_CREATE'),
    ('LEARNER', 'QA_ASK'),
    ('LEARNER', 'QA_ANSWER'),
    ('LEARNER', 'QA_MARK_USEFUL'),
    ('LEARNER', 'CONTENT_REPORT'),
    ('LEARNER', 'VERIFICATION_APPLY'),
    ('VERIFIED_CONTRIBUTOR', 'CONTENT_CREATE'),
    ('VERIFIED_CONTRIBUTOR', 'CONTENT_EDIT_OWN'),
    ('VERIFIED_CONTRIBUTOR', 'CONTENT_UPDATE_DRAFT'),
    ('VERIFIED_CONTRIBUTOR', 'CONTENT_SUBMIT'),
    ('CHECKER', 'CONTENT_REVIEW'),
    ('CHECKER', 'CONTENT_APPROVE'),
    ('CHECKER', 'CONTENT_REQUEST_CHANGES'),
    ('MODERATOR', 'CONTENT_MODERATE'),
    ('MODERATOR', 'CONTENT_REPORT_REVIEW'),
    ('ADMINISTRATOR', 'VERIFICATION_REVIEW'),
    ('ADMINISTRATOR', 'VERIFICATION_APPROVE'),
    ('ADMINISTRATOR', 'VERIFICATION_REVOKE'),
    ('ADMINISTRATOR', 'VERIFICATION_GRANT'),
    ('ADMINISTRATOR', 'TAXONOMY_MANAGE'),
    ('ADMINISTRATOR', 'USER_MANAGE'),
    ('ADMINISTRATOR', 'ROLE_MANAGE'),
    ('ADMINISTRATOR', 'ROLE_ASSIGN'),
    ('ADMINISTRATOR', 'SYSTEM_ADMIN'),
    ('ADMINISTRATOR', 'AUDIT_READ');

INSERT INTO competency (id, slug, name, description, active, created_at, updated_at) VALUES
    ('aaaaaaaa-0001-4000-8000-000000000001', 'matematika', 'Mathematics', 'Matematika', TRUE, NOW(), NOW()),
    ('aaaaaaaa-0001-4000-8000-000000000002', 'statistika', 'Statistics', 'Statistika', TRUE, NOW(), NOW()),
    ('aaaaaaaa-0001-4000-8000-000000000003', 'java', 'Java', 'Pemrograman Java', TRUE, NOW(), NOW()),
    ('aaaaaaaa-0001-4000-8000-000000000004', 'backend-development', 'Backend Development', 'Pengembangan backend', TRUE, NOW(), NOW()),
    ('aaaaaaaa-0001-4000-8000-000000000005', 'akuntansi', 'Accounting', 'Akuntansi', TRUE, NOW(), NOW()),
    ('aaaaaaaa-0001-4000-8000-000000000006', 'fisika', 'Physics', 'Fisika', TRUE, NOW(), NOW()),
    ('aaaaaaaa-0001-4000-8000-000000000007', 'bahasa-inggris', 'English Language', 'Bahasa Inggris', TRUE, NOW(), NOW());
