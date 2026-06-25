CREATE TYPE difficulty_enum AS ENUM (
    'EASY',
    'MEDIUM',
    'HARD'
);

CREATE TABLE problems(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR NOT NULL,
    description VARCHAR NOT NULL,
    difficulty VARCHAR(50) NOT NULL,
    test_cases JSONB,
    starter_code JSONB
);

CREATE TABLE matches(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    problem_id UUID REFERENCES problems,
    user1_id UUID REFERENCES users,
    user2_id UUID REFERENCES users,
    winner_id UUID REFERENCES users,
    status VARCHAR DEFAULT 'QUEUED',
    started_at TIMESTAMP,
    ended_at TIMESTAMP
);



