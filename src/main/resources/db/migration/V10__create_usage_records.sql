CREATE TABLE usage_records (
    id UUID NOT NULL PRIMARY KEY,
    organization_id UUID NOT NULL,
    usage_date DATE NOT NULL,
    emails_attempted INTEGER NOT NULL DEFAULT 0,
    emails_sent INTEGER NOT NULL DEFAULT 0,
    emails_failed INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT usage_records_org_fk FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT usage_records_unique UNIQUE (organization_id, usage_date),
    CONSTRAINT usage_records_counts_check CHECK (emails_attempted >= 0 AND emails_sent >= 0 AND emails_failed >= 0)
);

CREATE INDEX idx_usage_records_org_date ON usage_records (organization_id, usage_date);
