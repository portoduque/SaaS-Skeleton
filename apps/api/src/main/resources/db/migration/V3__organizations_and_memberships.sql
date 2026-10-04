CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT organizations_name_not_blank CHECK (btrim(name) <> '')
);

CREATE TABLE organization_memberships (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT organization_memberships_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
    CONSTRAINT organization_memberships_organization_user_unique UNIQUE (organization_id, user_id)
);

CREATE INDEX organization_memberships_user_organization_idx
    ON organization_memberships (user_id, organization_id);

CREATE UNIQUE INDEX organization_memberships_single_owner_idx
    ON organization_memberships (organization_id)
    WHERE role = 'OWNER';
