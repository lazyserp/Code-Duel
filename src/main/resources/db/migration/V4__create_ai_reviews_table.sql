CREATE TABLE ai_hints(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id UUID REFERENCES submissions(id),
    review_type VARCHAR,
    feedback_text TEXT,
    model_used VARCHAR,
    tokens_consumed INTEGER,
    created_at TIMESTAMP
);

CREATE INDEX idx_ai_hints_submission ON ai_hints(submission_id);