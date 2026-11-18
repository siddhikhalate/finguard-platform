-- ==============================================================================
-- FinGuard Fraud Detection Service - Database Migration V1
-- ==============================================================================

-- 1. Create Fraud Rules Table
CREATE TABLE fraud_rules (
    id UUID PRIMARY KEY,
    rule_code VARCHAR(50) NOT NULL UNIQUE,
    rule_name VARCHAR(100) NOT NULL,
    description TEXT,
    rule_type VARCHAR(50) NOT NULL,
    weight INT NOT NULL,
    threshold_value NUMERIC(15, 2) NOT NULL,
    time_window_seconds INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fraud_rules_code ON fraud_rules(rule_code);
CREATE INDEX idx_fraud_rules_active ON fraud_rules(is_active);

-- 2. Create Risk Assessments Table
CREATE TABLE risk_assessments (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL UNIQUE,
    account_id UUID NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    risk_score INT NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    action_recommended VARCHAR(50) NOT NULL,
    evaluation_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_risk_txn_id ON risk_assessments(transaction_id);
CREATE INDEX idx_risk_level ON risk_assessments(risk_level);
CREATE INDEX idx_risk_eval_time ON risk_assessments(evaluation_timestamp DESC);

-- 3. Create Triggered Rules Audit Table
CREATE TABLE triggered_rules (
    id UUID PRIMARY KEY,
    assessment_id UUID NOT NULL,
    rule_code VARCHAR(50) NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    points_added INT NOT NULL,
    trigger_reason TEXT NOT NULL,
    CONSTRAINT fk_triggered_rules_assessment FOREIGN KEY (assessment_id) REFERENCES risk_assessments(id) ON DELETE CASCADE
);

CREATE INDEX idx_triggered_rules_assessment ON triggered_rules(assessment_id);

-- 4. Seed Deterministic Fraud Detection Rules
INSERT INTO fraud_rules (id, rule_code, rule_name, description, rule_type, weight, threshold_value, time_window_seconds, is_active)
VALUES
('c0000000-0000-0000-0000-000000000001', 'RULE_HIGH_AMOUNT', 'High Transaction Amount', 'Flags single transactions exceeding threshold limit', 'AMOUNT_THRESHOLD', 25, 10000.00, 0, true),
('c0000000-0000-0000-0000-000000000002', 'RULE_RAPID_ACTIVITY', 'Rapid Transaction Velocity', 'Detects multiple rapid transactions from the same account within a sliding time window', 'RAPID_ACTIVITY', 25, 3.00, 120, true),
('c0000000-0000-0000-0000-000000000003', 'RULE_UNUSUAL_LOCATION', 'Unusual Geographic Location', 'Flags transactions originating from high-risk or unexpected international locations', 'LOCATION_DEVIATION', 20, 1.00, 0, true),
('c0000000-0000-0000-0000-000000000004', 'RULE_REPEATED_FAILURES', 'Failed Transaction Attempt', 'Detects repeated failed transactions indicating brute-force card probing', 'FAILED_ATTEMPTS', 15, 1.00, 300, true)
ON CONFLICT (rule_code) DO NOTHING;
