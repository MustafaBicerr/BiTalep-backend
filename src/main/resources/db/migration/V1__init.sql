-- BiTalep SaaS schema. UUID PKs, tenant isolation, audit + soft delete.

CREATE TABLE tenants (
    id              UUID PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL,
    created_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL,
    updated_by      UUID,
    deleted_at      TIMESTAMPTZ
);

CREATE TABLE users (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants (id),
    name            VARCHAR(100) NOT NULL,
    surname         VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(32) NOT NULL CHECK (role IN ('PERSONEL', 'ADMIN')),
    department      VARCHAR(32) NOT NULL CHECK (department IN (
                        'HR', 'IT', 'FINANCE', 'SALES', 'OPERATIONS', 'MARKETING', 'OTHER'
                    )),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL,
    created_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL,
    updated_by      UUID,
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_users_tenant_id ON users (tenant_id, id);
CREATE UNIQUE INDEX uq_users_tenant_email_live ON users (tenant_id, lower(email)) WHERE deleted_at IS NULL;

CREATE TABLE subscriptions (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL REFERENCES tenants (id),
    plan                VARCHAR(32) NOT NULL CHECK (plan = 'PRO'),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    current_period_end  TIMESTAMPTZ NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL,
    created_by          UUID,
    updated_at          TIMESTAMPTZ NOT NULL,
    updated_by          UUID,
    deleted_at          TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_subscriptions_tenant_live ON subscriptions (tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_subscriptions_tenant_id ON subscriptions (tenant_id, id);

CREATE TABLE form_types (
    id          UUID PRIMARY KEY,
    code        VARCHAR(32) NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    created_by  UUID,
    updated_at  TIMESTAMPTZ NOT NULL,
    updated_by  UUID
);

INSERT INTO form_types (id, code, name, created_at, created_by, updated_at, updated_by) VALUES
    ('11111111-1111-1111-1111-111111111001', 'LEAVE',     'İzin',     TIMESTAMPTZ '2026-01-01 00:00:00+00', NULL, TIMESTAMPTZ '2026-01-01 00:00:00+00', NULL),
    ('11111111-1111-1111-1111-111111111002', 'TRAINING',  'Eğitim',   TIMESTAMPTZ '2026-01-01 00:00:00+00', NULL, TIMESTAMPTZ '2026-01-01 00:00:00+00', NULL),
    ('11111111-1111-1111-1111-111111111003', 'ADVANCE',   'Avans',    TIMESTAMPTZ '2026-01-01 00:00:00+00', NULL, TIMESTAMPTZ '2026-01-01 00:00:00+00', NULL),
    ('11111111-1111-1111-1111-111111111004', 'MATERIAL',  'Malzeme',  TIMESTAMPTZ '2026-01-01 00:00:00+00', NULL, TIMESTAMPTZ '2026-01-01 00:00:00+00', NULL),
    ('11111111-1111-1111-1111-111111111005', 'TASK',      'Görev',    TIMESTAMPTZ '2026-01-01 00:00:00+00', NULL, TIMESTAMPTZ '2026-01-01 00:00:00+00', NULL);

CREATE TABLE applications (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants (id),
    applicant_id    UUID NOT NULL REFERENCES users (id),
    title           VARCHAR(100) NOT NULL,
    description     VARCHAR(1000) NOT NULL,
    form_type       VARCHAR(32) NOT NULL CHECK (form_type IN ('LEAVE', 'TRAINING', 'ADVANCE', 'MATERIAL', 'TASK')),
    status          VARCHAR(32) NOT NULL CHECK (status IN ('NEW', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'CANCELLED')),
    reject_reason   VARCHAR(1000),
    created_at      TIMESTAMPTZ NOT NULL,
    created_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL,
    updated_by      UUID,
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_applications_tenant_id ON applications (tenant_id, id);
CREATE INDEX idx_applications_tenant_status ON applications (tenant_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_applications_tenant_applicant ON applications (tenant_id, applicant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_applications_created_at ON applications (tenant_id, created_at DESC);

CREATE TABLE application_events (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants (id),
    application_id  UUID NOT NULL REFERENCES applications (id),
    status          VARCHAR(32) NOT NULL,
    description     VARCHAR(1000),
    actor_id        UUID REFERENCES users (id),
    created_at      TIMESTAMPTZ NOT NULL,
    created_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL,
    updated_by      UUID,
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_application_events_app ON application_events (tenant_id, application_id, created_at);

CREATE TABLE attachments (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants (id),
    application_id  UUID NOT NULL REFERENCES applications (id),
    file_name       VARCHAR(255) NOT NULL,
    original_name   VARCHAR(255) NOT NULL,
    file_path       VARCHAR(1024) NOT NULL,
    file_size       BIGINT NOT NULL,
    mime_type       VARCHAR(128) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    created_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL,
    updated_by      UUID,
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_attachments_tenant_id ON attachments (tenant_id, id);
CREATE INDEX idx_attachments_application ON attachments (tenant_id, application_id) WHERE deleted_at IS NULL;

CREATE TABLE notifications (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL REFERENCES tenants (id),
    recipient_id        UUID NOT NULL REFERENCES users (id),
    actor_id            UUID REFERENCES users (id),
    related_request_id  UUID REFERENCES applications (id),
    type                VARCHAR(32) NOT NULL CHECK (type IN (
                            'STATUS_CHANGE', 'APPROVED', 'REJECTED', 'NEW_REQUEST', 'FILE_UPLOADED', 'SYSTEM'
                        )),
    title               VARCHAR(200) NOT NULL,
    description         VARCHAR(1000) NOT NULL,
    is_read             BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL,
    created_by          UUID,
    updated_at          TIMESTAMPTZ NOT NULL,
    updated_by          UUID,
    deleted_at          TIMESTAMPTZ
);
CREATE INDEX idx_notifications_recipient ON notifications (tenant_id, recipient_id, is_read, created_at DESC) WHERE deleted_at IS NULL;

CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants (id),
    user_id         UUID NOT NULL REFERENCES users (id),
    family_id       UUID NOT NULL,
    token_hash      VARCHAR(128) NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked_at      TIMESTAMPTZ,
    replaced_by     UUID,
    created_at      TIMESTAMPTZ NOT NULL,
    created_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL,
    updated_by      UUID,
    deleted_at      TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_refresh_token_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_family ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_user ON refresh_tokens (tenant_id, user_id);

CREATE TABLE password_reset_tokens (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL REFERENCES tenants (id),
    user_id         UUID NOT NULL REFERENCES users (id),
    token_hash      VARCHAR(128) NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    used_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL,
    created_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL,
    updated_by      UUID,
    deleted_at      TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_password_reset_hash ON password_reset_tokens (token_hash);
CREATE INDEX idx_password_reset_user ON password_reset_tokens (tenant_id, user_id);
