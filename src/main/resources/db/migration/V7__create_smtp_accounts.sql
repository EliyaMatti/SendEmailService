CREATE TABLE smtp_accounts (
    id UUID NOT NULL PRIMARY KEY,
    organization_id UUID NOT NULL,
    provider VARCHAR(64) NOT NULL,
    host VARCHAR(255) NOT NULL,
    port INTEGER NOT NULL,
    username VARCHAR(320) NOT NULL,
    encrypted_password TEXT NOT NULL,
    key_version INTEGER NOT NULL,
    from_email VARCHAR(320) NOT NULL,
    from_name VARCHAR(255),
    tls_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT smtp_accounts_org_fk FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT smtp_accounts_port_check CHECK (port > 0 AND port <= 65535),
    CONSTRAINT smtp_accounts_key_version_check CHECK (key_version >= 1)
);

CREATE INDEX idx_smtp_accounts_org_id ON smtp_accounts (organization_id);
