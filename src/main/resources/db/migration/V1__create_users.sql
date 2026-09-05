CREATE TABLE users (
    id UUID NOT NULL PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT users_email_key UNIQUE (email),
    CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE INDEX idx_users_email ON users (email);
