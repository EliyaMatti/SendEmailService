CREATE TABLE contact_lists (
    id UUID NOT NULL PRIMARY KEY,
    organization_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    source_filename VARCHAR(512),
    total_contacts INTEGER NOT NULL DEFAULT 0,
    placeholder_keys TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT contact_lists_org_fk FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT contact_lists_total_check CHECK (total_contacts >= 0)
);

CREATE INDEX idx_contact_lists_org_id ON contact_lists (organization_id);
