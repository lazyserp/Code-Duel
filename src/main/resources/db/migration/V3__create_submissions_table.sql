CREATE TABLE submissions(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID REFERENCES matches,
    user_id UUID REFERENCES users,
    problem_id UUID REFERENCES problems,
    code_text TEXT NOT NULL,
    language VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    execution_time INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_submissions_match_user_status ON submissions(match_id, user_id, status);