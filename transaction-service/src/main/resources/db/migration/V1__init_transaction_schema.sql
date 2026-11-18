-- ==============================================================================
-- FinGuard Transaction Service - Database Migration V1
-- ==============================================================================

-- 1. Create Accounts Table
CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    account_number VARCHAR(50) NOT NULL UNIQUE,
    cardholder_name VARCHAR(100) NOT NULL,
    balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_accounts_number ON accounts(account_number);

-- 2. Create Transactions Table
CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    account_id UUID NOT NULL,
    source_account VARCHAR(50) NOT NULL,
    target_account VARCHAR(50) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    channel VARCHAR(50) NOT NULL,
    location VARCHAR(100) NOT NULL,
    ip_address VARCHAR(45),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE INDEX idx_txn_account_created ON transactions(account_id, created_at DESC);
CREATE INDEX idx_txn_idempotency ON transactions(idempotency_key);
CREATE INDEX idx_txn_status ON transactions(status);

-- 3. Create Transactional Outbox Events Table
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_outbox_status_created ON outbox_events(status, created_at);

-- 4. Seed Standard Testing Accounts
INSERT INTO accounts (id, account_number, cardholder_name, balance, currency, status)
VALUES
('b0000000-0000-0000-0000-000000000001', 'ACC-100201', 'Alice Walker (Premium Trader)', 75000.00, 'USD', 'ACTIVE'),
('b0000000-0000-0000-0000-000000000002', 'ACC-100202', 'Bob Martin (Retail Cardholder)', 3500.00, 'USD', 'ACTIVE'),
('b0000000-0000-0000-0000-000000000003', 'ACC-100203', 'Apex Corp Treasury', 500000.00, 'USD', 'ACTIVE')
ON CONFLICT (account_number) DO NOTHING;
