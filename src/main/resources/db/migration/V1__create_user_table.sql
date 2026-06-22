
CREATE TABLE users(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR NOT NULL UNIQUE,
    password_hash VARCHAR NOT NULL ,
    username VARCHAR NOT NULL UNIQUE,
    current_elo INTEGER DEFAULT 1200,
    is_online BOOLEAN NOT NULL DEFAULT FALSE
)

