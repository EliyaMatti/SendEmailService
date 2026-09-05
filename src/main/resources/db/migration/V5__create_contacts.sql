CREATE TABLE contacts (
    id UUID NOT NULL PRIMARY KEY,
    contact_list_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    email VARCHAR(320) NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    metadata_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT contacts_list_fk FOREIGN KEY (contact_list_id) REFERENCES contact_lists (id) ON DELETE CASCADE,
    CONSTRAINT contacts_org_fk FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT contacts_status_check CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT contacts_list_email_unique UNIQUE (contact_list_id, email)
);

CREATE INDEX idx_contacts_org_id ON contacts (organization_id);
CREATE INDEX idx_contacts_list_id ON contacts (contact_list_id);
CREATE INDEX idx_contacts_org_email ON contacts (organization_id, email);
