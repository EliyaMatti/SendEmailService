CREATE TABLE organizations (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    owner_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT organizations_owner_fk FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT organizations_status_check CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE INDEX idx_organizations_owner_id ON organizations (owner_id);
