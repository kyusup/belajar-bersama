-- Community learning Q&A and report queue indexes.
CREATE TABLE qa_question (
    id UUID PRIMARY KEY,
    author_id UUID NOT NULL REFERENCES app_user (id),
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    subject_id UUID NULL REFERENCES subject (id),
    content_id UUID NULL REFERENCES educational_content (id),
    status VARCHAR(16) NOT NULL,
    accepted_answer_id UUID NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT qa_question_status_chk CHECK (status IN ('OPEN', 'CLOSED', 'HIDDEN'))
);

CREATE INDEX idx_qa_question_created ON qa_question (created_at DESC);
CREATE INDEX idx_qa_question_content ON qa_question (content_id) WHERE content_id IS NOT NULL;
CREATE INDEX idx_qa_question_status ON qa_question (status, created_at DESC);

CREATE TABLE qa_answer (
    id UUID PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES qa_question (id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES app_user (id),
    body TEXT NOT NULL,
    hidden BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_qa_answer_question ON qa_answer (question_id, created_at);

ALTER TABLE qa_question
    ADD CONSTRAINT qa_question_accepted_fk
    FOREIGN KEY (accepted_answer_id) REFERENCES qa_answer (id) ON DELETE SET NULL;

CREATE TABLE qa_answer_useful (
    user_id UUID NOT NULL REFERENCES app_user (id),
    answer_id UUID NOT NULL REFERENCES qa_answer (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, answer_id)
);

CREATE TABLE qa_report (
    id UUID PRIMARY KEY,
    reporter_id UUID NOT NULL REFERENCES app_user (id),
    target_type VARCHAR(16) NOT NULL,
    target_id UUID NOT NULL,
    reason VARCHAR(32) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT qa_report_type_chk CHECK (target_type IN ('QUESTION', 'ANSWER')),
    CONSTRAINT qa_report_status_chk CHECK (status IN ('OPEN', 'UNDER_REVIEW', 'RESOLVED', 'DISMISSED'))
);

CREATE UNIQUE INDEX uq_qa_report_open
    ON qa_report (reporter_id, target_type, target_id)
    WHERE status IN ('OPEN', 'UNDER_REVIEW');

CREATE INDEX idx_qa_report_status ON qa_report (status, created_at DESC);

CREATE TABLE qa_search (
    question_id UUID PRIMARY KEY REFERENCES qa_question (id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    body_text TEXT NOT NULL,
    document TSVECTOR GENERATED ALWAYS AS (
        setweight(to_tsvector('simple', coalesce(title, '')), 'A')
        || setweight(to_tsvector('simple', coalesce(body_text, '')), 'B')
    ) STORED
);

CREATE INDEX idx_qa_search_document ON qa_search USING GIN (document);
