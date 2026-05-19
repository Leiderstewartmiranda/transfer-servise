-- =====================================================
-- Schema inicial para PostgreSQL (producción)
-- Se ejecuta automáticamente al crear el contenedor
-- =====================================================

CREATE TABLE IF NOT EXISTS accounts (
    id          VARCHAR(36)    PRIMARY KEY,
    owner_name  VARCHAR(100)   NOT NULL,
    balance     NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    currency    VARCHAR(3)     NOT NULL DEFAULT 'COP',
    status      VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS transfers (
    id                VARCHAR(36)    PRIMARY KEY,
    source_account_id VARCHAR(36)    NOT NULL,
    target_account_id VARCHAR(36)    NOT NULL,
    amount            NUMERIC(19, 2) NOT NULL,
    currency          VARCHAR(3)     NOT NULL,
    description       VARCHAR(255),
    status            VARCHAR(20)    NOT NULL,
    created_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP,

    CONSTRAINT fk_source FOREIGN KEY (source_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_target FOREIGN KEY (target_account_id) REFERENCES accounts(id)
);

CREATE INDEX IF NOT EXISTS idx_transfers_source ON transfers(source_account_id);
CREATE INDEX IF NOT EXISTS idx_transfers_target ON transfers(target_account_id);
CREATE INDEX IF NOT EXISTS idx_transfers_status ON transfers(status);

