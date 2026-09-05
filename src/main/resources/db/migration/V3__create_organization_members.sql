CREATE TABLE organization_members (
    id UUID NOT NULL PRIMARY KEY,
    organization_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT organization_members_org_fk FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT organization_members_user_fk FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT organization_members_role_check CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
    CONSTRAINT organization_members_unique UNIQUE (organization_id, user_id)
);

CREATE INDEX idx_organization_members_org_id ON organization_members (organization_id);
CREATE INDEX idx_organization_members_user_id ON organization_members (user_id);
