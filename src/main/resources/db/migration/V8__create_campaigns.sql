CREATE TABLE campaigns (
    id UUID NOT NULL PRIMARY KEY,
    organization_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    contact_list_id UUID NOT NULL,
    template_id UUID NOT NULL,
    smtp_account_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_recipients INTEGER NOT NULL DEFAULT 0,
    queued_count INTEGER NOT NULL DEFAULT 0,
    sent_count INTEGER NOT NULL DEFAULT 0,
    failed_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT campaigns_org_fk FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT campaigns_list_fk FOREIGN KEY (contact_list_id) REFERENCES contact_lists (id),
    CONSTRAINT campaigns_template_fk FOREIGN KEY (template_id) REFERENCES email_templates (id),
    CONSTRAINT campaigns_smtp_fk FOREIGN KEY (smtp_account_id) REFERENCES smtp_accounts (id),
    CONSTRAINT campaigns_status_check CHECK (status IN ('DRAFT', 'READY', 'RUNNING', 'PAUSED', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT campaigns_counts_check CHECK (total_recipients >= 0 AND queued_count >= 0 AND sent_count >= 0 AND failed_count >= 0)
);

CREATE INDEX idx_campaigns_org_id ON campaigns (organization_id);
CREATE INDEX idx_campaigns_org_status ON campaigns (organization_id, status);
