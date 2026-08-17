-- Learning structure ordering, quizzes, attempts, progress, bookmarks.
ALTER TABLE educational_content DROP CONSTRAINT educational_content_kind_chk;
ALTER TABLE educational_content ADD CONSTRAINT educational_content_kind_chk CHECK (
    kind IN ('LEARNING_PATH', 'COURSE', 'MODULE', 'LESSON', 'MATERIAL', 'QUIZ')
);

ALTER TABLE educational_content ADD COLUMN sort_order INT NOT NULL DEFAULT 0;
ALTER TABLE educational_content ADD COLUMN required BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_educational_content_parent_order ON educational_content (parent_id, sort_order);

CREATE TABLE quiz_spec (
    revision_id UUID PRIMARY KEY REFERENCES content_revision (id) ON DELETE CASCADE,
    passing_score INT NULL,
    max_attempts INT NULL,
    required BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT quiz_spec_passing_chk CHECK (passing_score IS NULL OR (passing_score >= 0 AND passing_score <= 100)),
    CONSTRAINT quiz_spec_attempts_chk CHECK (max_attempts IS NULL OR max_attempts >= 1)
);

CREATE TABLE quiz_question (
    id UUID PRIMARY KEY,
    revision_id UUID NOT NULL REFERENCES content_revision (id) ON DELETE CASCADE,
    sort_order INT NOT NULL,
    type VARCHAR(32) NOT NULL,
    prompt TEXT NOT NULL,
    explanation TEXT NULL,
    difficulty VARCHAR(16) NOT NULL,
    competency_id UUID NULL REFERENCES competency (id),
    reference_note TEXT NULL,
    CONSTRAINT quiz_question_type_chk CHECK (type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE')),
    CONSTRAINT quiz_question_difficulty_chk CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD'))
);

CREATE INDEX idx_quiz_question_revision ON quiz_question (revision_id, sort_order);

CREATE TABLE quiz_option (
    id UUID PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES quiz_question (id) ON DELETE CASCADE,
    sort_order INT NOT NULL,
    label VARCHAR(32) NOT NULL,
    body TEXT NOT NULL,
    correct BOOLEAN NOT NULL
);

CREATE INDEX idx_quiz_option_question ON quiz_option (question_id, sort_order);

CREATE TABLE quiz_attempt (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user (id),
    quiz_id UUID NOT NULL REFERENCES educational_content (id),
    quiz_revision_id UUID NOT NULL REFERENCES content_revision (id),
    status VARCHAR(32) NOT NULL,
    score_percent INT NULL,
    passed BOOLEAN NULL,
    correct_count INT NULL,
    question_count INT NULL,
    version INT NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ NOT NULL,
    submitted_at TIMESTAMPTZ NULL,
    CONSTRAINT quiz_attempt_status_chk CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'ABANDONED'))
);

CREATE INDEX idx_quiz_attempt_user ON quiz_attempt (user_id, started_at DESC);
CREATE INDEX idx_quiz_attempt_quiz ON quiz_attempt (quiz_id, user_id);
CREATE UNIQUE INDEX uq_quiz_attempt_open
    ON quiz_attempt (user_id, quiz_id)
    WHERE status = 'IN_PROGRESS';

CREATE TABLE quiz_answer_option (
    attempt_id UUID NOT NULL REFERENCES quiz_attempt (id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES quiz_question (id),
    option_id UUID NOT NULL REFERENCES quiz_option (id),
    PRIMARY KEY (attempt_id, question_id, option_id)
);

CREATE TABLE lesson_completion (
    user_id UUID NOT NULL REFERENCES app_user (id),
    content_id UUID NOT NULL REFERENCES educational_content (id),
    revision_id UUID NOT NULL REFERENCES content_revision (id),
    completed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, content_id)
);

CREATE INDEX idx_lesson_completion_content ON lesson_completion (content_id);

CREATE TABLE bookmark (
    user_id UUID NOT NULL REFERENCES app_user (id),
    content_id UUID NOT NULL REFERENCES educational_content (id),
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, content_id)
);

CREATE INDEX idx_bookmark_user ON bookmark (user_id, created_at DESC);

CREATE TABLE learning_activity (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user (id),
    content_id UUID NULL REFERENCES educational_content (id),
    kind VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT learning_activity_kind_chk CHECK (
        kind IN ('COURSE_STARTED', 'LESSON_COMPLETED', 'QUIZ_STARTED', 'QUIZ_SUBMITTED', 'BOOKMARK_CREATED')
    )
);

CREATE INDEX idx_learning_activity_user ON learning_activity (user_id, created_at DESC);

CREATE TABLE learning_resume (
    user_id UUID PRIMARY KEY REFERENCES app_user (id),
    content_id UUID NOT NULL REFERENCES educational_content (id),
    course_id UUID NULL REFERENCES educational_content (id),
    updated_at TIMESTAMPTZ NOT NULL
);
