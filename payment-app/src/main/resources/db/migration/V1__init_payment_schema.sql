-- V1: Initial Payment Schema
CREATE TABLE IF NOT EXISTS payments (
    id VARCHAR(36) PRIMARY KEY,
    from_account VARCHAR(64) NOT NULL,
    to_account VARCHAR(64) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    standard VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    description VARCHAR(255),
    reference_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT chk_payment_amount CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_payments_from_account ON payments(from_account);
CREATE INDEX IF NOT EXISTS idx_payments_reference_id ON payments(reference_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at DESC);
