CREATE TABLE campaign_recipients (
    id UUID NOT NULL PRIMARY KEY,
    campaign_id UUID NOT NULL,
    contact_id UUID NOT NULL,
    email VARCHAR(320) NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(512),
    queued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT campaign_recipients_campaign_fk FOREIGN KEY (campaign_id) REFERENCES campaigns (id) ON DELETE CASCADE,
    CONSTRAINT campaign_recipients_contact_fk FOREIGN KEY (contact_id) REFERENCES contacts (id),
    CONSTRAINT campaign_recipients_status_check CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'SKIPPED')),
    CONSTRAINT campaign_recipients_attempts_check CHECK (attempt_count >= 0),
    CONSTRAINT campaign_recipients_unique UNIQUE (campaign_id, contact_id)
);

CREATE INDEX idx_campaign_recipients_campaign_id ON campaign_recipients (campaign_id);
CREATE INDEX idx_campaign_recipients_campaign_status ON campaign_recipients (campaign_id, status);
