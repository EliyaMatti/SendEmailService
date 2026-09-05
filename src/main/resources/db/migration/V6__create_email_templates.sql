CREATE TABLE email_templates (
    id UUID NOT NULL PRIMARY KEY,
    organization_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    subject VARCHAR(998) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT email_templates_org_fk FOREIGN KEY (organization_id) REFERENCES organizations (id)
);

CREATE INDEX idx_email_templates_org_id ON email_templates (organization_id);
