-- Educational content, taxonomy, revisions, maker-checker workflow, reports, search.
CREATE TABLE subject (
    id UUID PRIMARY KEY,
    slug VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE education_level (
    id UUID PRIMARY KEY,
    slug VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    sort_order INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE license (
    code VARCHAR(64) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description TEXT NOT NULL
);

CREATE TABLE educational_content (
    id UUID PRIMARY KEY,
    kind VARCHAR(32) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    maker_id UUID NOT NULL REFERENCES app_user (id),
    subject_id UUID NOT NULL REFERENCES subject (id),
    education_level_id UUID NOT NULL REFERENCES education_level (id),
    parent_id UUID NULL REFERENCES educational_content (id),
    status VARCHAR(32) NOT NULL,
    current_revision_id UUID NULL,
    published_revision_id UUID NULL,
    archived_at TIMESTAMPTZ NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT educational_content_kind_chk CHECK (
        kind IN ('LEARNING_PATH', 'COURSE', 'MODULE', 'LESSON', 'MATERIAL')
    ),
    CONSTRAINT educational_content_status_chk CHECK (
        status IN (
            'DRAFT',
            'SUBMITTED',
            'IN_REVIEW',
            'CHANGES_REQUESTED',
            'APPROVED',
            'PUBLISHED',
            'ARCHIVED'
        )
    )
);

CREATE INDEX idx_educational_content_maker ON educational_content (maker_id);
CREATE INDEX idx_educational_content_subject ON educational_content (subject_id);
CREATE INDEX idx_educational_content_status ON educational_content (status);
CREATE INDEX idx_educational_content_parent ON educational_content (parent_id);
CREATE INDEX idx_educational_content_public ON educational_content (subject_id)
    WHERE published_revision_id IS NOT NULL AND archived_at IS NULL;

CREATE TABLE content_slug_history (
    slug VARCHAR(120) PRIMARY KEY,
    content_id UUID NOT NULL REFERENCES educational_content (id),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_content_slug_history_content ON content_slug_history (content_id);

CREATE TABLE content_revision (
    id UUID PRIMARY KEY,
    content_id UUID NOT NULL REFERENCES educational_content (id),
    revision_number INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    summary TEXT NOT NULL,
    body JSONB NOT NULL,
    license_code VARCHAR(64) NOT NULL REFERENCES license (code),
    change_summary TEXT NULL,
    created_by UUID NOT NULL REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT content_revision_unique UNIQUE (content_id, revision_number)
);

CREATE INDEX idx_content_revision_content ON content_revision (content_id);

ALTER TABLE educational_content
    ADD CONSTRAINT educational_content_current_revision_fk
    FOREIGN KEY (current_revision_id) REFERENCES content_revision (id);
ALTER TABLE educational_content
    ADD CONSTRAINT educational_content_published_revision_fk
    FOREIGN KEY (published_revision_id) REFERENCES content_revision (id);

CREATE TABLE content_revision_competency (
    revision_id UUID NOT NULL REFERENCES content_revision (id) ON DELETE CASCADE,
    competency_id UUID NOT NULL REFERENCES competency (id),
    PRIMARY KEY (revision_id, competency_id)
);

CREATE TABLE content_source (
    id UUID PRIMARY KEY,
    revision_id UUID NOT NULL REFERENCES content_revision (id) ON DELETE CASCADE,
    title VARCHAR(300) NOT NULL,
    author VARCHAR(200) NULL,
    publisher VARCHAR(200) NULL,
    url TEXT NULL,
    publication_info TEXT NULL,
    notes TEXT NULL,
    sort_order INT NOT NULL
);

CREATE INDEX idx_content_source_revision ON content_source (revision_id);

CREATE TABLE content_submission (
    id UUID PRIMARY KEY,
    content_id UUID NOT NULL REFERENCES educational_content (id),
    revision_id UUID NOT NULL REFERENCES content_revision (id),
    maker_id UUID NOT NULL REFERENCES app_user (id),
    status VARCHAR(32) NOT NULL,
    assigned_checker_id UUID NULL REFERENCES app_user (id),
    assigned_by UUID NULL REFERENCES app_user (id),
    assigned_at TIMESTAMPTZ NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT content_submission_status_chk CHECK (
        status IN ('SUBMITTED', 'IN_REVIEW', 'CHANGES_REQUESTED', 'APPROVED')
    )
);

CREATE INDEX idx_content_submission_content ON content_submission (content_id);
CREATE INDEX idx_content_submission_status ON content_submission (status);
CREATE UNIQUE INDEX uq_content_submission_open
    ON content_submission (content_id)
    WHERE status IN ('SUBMITTED', 'IN_REVIEW');

CREATE TABLE content_review (
    id UUID PRIMARY KEY,
    submission_id UUID NOT NULL REFERENCES content_submission (id),
    revision_id UUID NOT NULL REFERENCES content_revision (id),
    reviewer_id UUID NOT NULL REFERENCES app_user (id),
    decision VARCHAR(32) NULL,
    comment TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    decided_at TIMESTAMPTZ NULL,
    CONSTRAINT content_review_decision_chk CHECK (
        decision IS NULL OR decision IN ('APPROVE', 'REQUEST_CHANGES')
    )
);

CREATE INDEX idx_content_review_submission ON content_review (submission_id);
CREATE INDEX idx_content_review_reviewer ON content_review (reviewer_id);

CREATE TABLE content_report (
    id UUID PRIMARY KEY,
    content_id UUID NOT NULL REFERENCES educational_content (id),
    reporter_id UUID NOT NULL REFERENCES app_user (id),
    reason VARCHAR(32) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT content_report_reason_chk CHECK (
        reason IN ('INCORRECT', 'COPYRIGHT', 'INAPPROPRIATE', 'SPAM', 'OTHER')
    ),
    CONSTRAINT content_report_status_chk CHECK (
        status IN ('OPEN', 'UNDER_REVIEW', 'RESOLVED', 'DISMISSED')
    )
);

CREATE INDEX idx_content_report_content ON content_report (content_id);
CREATE UNIQUE INDEX uq_content_report_open
    ON content_report (reporter_id, content_id)
    WHERE status IN ('OPEN', 'UNDER_REVIEW');

CREATE TABLE content_search (
    content_id UUID PRIMARY KEY REFERENCES educational_content (id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    summary TEXT NOT NULL,
    body_text TEXT NOT NULL,
    subject_name TEXT NOT NULL,
    document TSVECTOR GENERATED ALWAYS AS (
        setweight(to_tsvector('simple', coalesce(title, '')), 'A')
        || setweight(to_tsvector('simple', coalesce(summary, '')), 'B')
        || setweight(to_tsvector('simple', coalesce(body_text, '')), 'C')
        || setweight(to_tsvector('simple', coalesce(subject_name, '')), 'A')
    ) STORED
);

CREATE INDEX idx_content_search_document ON content_search USING GIN (document);

INSERT INTO license (code, name, description) VALUES
    ('CC_BY_SA', 'CC BY-SA', 'Creative Commons Attribution-ShareAlike. Recommended default for original work the contributor has the right to license.'),
    ('PUBLIC_DOMAIN', 'Public Domain', 'Work dedicated to the public domain, or with no known copyright restrictions.'),
    ('ORIGINAL_WORK', 'Original Work', 'Original work of the contributor. Selecting a license does not grant rights the contributor does not possess.'),
    ('EXTERNAL_ALL_RIGHTS_RESERVED', 'External / All Rights Reserved', 'Third-party or reserved-rights material. Do not claim ownership of material you do not own.'),
    ('OTHER', 'Other / Specified', 'Other terms described in sources or notes.');

INSERT INTO subject (id, slug, name, description, active, created_at, updated_at) VALUES
    ('bbbbbbbb-0001-4000-8000-000000000001', 'matematika', 'Matematika', 'Matematika', TRUE, NOW(), NOW()),
    ('bbbbbbbb-0001-4000-8000-000000000002', 'statistika', 'Statistika', 'Statistika', TRUE, NOW(), NOW()),
    ('bbbbbbbb-0001-4000-8000-000000000003', 'pemrograman', 'Pemrograman', 'Pemrograman dan rekayasa perangkat lunak', TRUE, NOW(), NOW()),
    ('bbbbbbbb-0001-4000-8000-000000000004', 'akuntansi', 'Akuntansi', 'Akuntansi', TRUE, NOW(), NOW()),
    ('bbbbbbbb-0001-4000-8000-000000000005', 'fisika', 'Fisika', 'Fisika', TRUE, NOW(), NOW()),
    ('bbbbbbbb-0001-4000-8000-000000000006', 'bahasa-inggris', 'Bahasa Inggris', 'Bahasa Inggris', TRUE, NOW(), NOW());

INSERT INTO education_level (id, slug, name, sort_order, active, created_at, updated_at) VALUES
    ('cccccccc-0001-4000-8000-000000000001', 'sd', 'SD', 10, TRUE, NOW(), NOW()),
    ('cccccccc-0002-4000-8000-000000000001', 'smp', 'SMP', 20, TRUE, NOW(), NOW()),
    ('cccccccc-0003-4000-8000-000000000001', 'sma', 'SMA', 30, TRUE, NOW(), NOW()),
    ('cccccccc-0004-4000-8000-000000000001', 'smk', 'SMK', 40, TRUE, NOW(), NOW()),
    ('cccccccc-0005-4000-8000-000000000001', 'perguruan-tinggi', 'Perguruan Tinggi', 50, TRUE, NOW(), NOW()),
    ('cccccccc-0006-4000-8000-000000000001', 'umum', 'Umum', 60, TRUE, NOW(), NOW()),
    ('cccccccc-0007-4000-8000-000000000001', 'profesional', 'Profesional', 70, TRUE, NOW(), NOW()),
    ('cccccccc-0008-4000-8000-000000000001', 'keterampilan', 'Keterampilan', 80, TRUE, NOW(), NOW());

INSERT INTO role_permission (role_id, permission_id) VALUES
    ('VERIFIED_CONTRIBUTOR', 'CONTENT_PUBLISH'),
    ('VERIFIED_CONTRIBUTOR', 'CONTENT_ARCHIVE'),
    ('ADMINISTRATOR', 'CONTENT_ARCHIVE'),
    ('ADMINISTRATOR', 'CONTENT_PUBLISH')
ON CONFLICT DO NOTHING;
