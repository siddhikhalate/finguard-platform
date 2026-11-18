-- ==============================================================================
-- FinGuard Incident Management Service - Database Migration V1
-- ==============================================================================

-- 1. Create Fraud Incidents Table
CREATE TABLE fraud_incidents (
    id UUID PRIMARY KEY,
    incident_reference VARCHAR(50) NOT NULL UNIQUE,
    transaction_id UUID NOT NULL UNIQUE,
    account_id UUID NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    location VARCHAR(100),
    risk_score INT NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    priority VARCHAR(20) NOT NULL DEFAULT 'HIGH',
    assigned_to UUID,
    assigned_to_username VARCHAR(50),
    resolution_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_incidents_status ON fraud_incidents(status);
CREATE INDEX idx_incidents_priority ON fraud_incidents(priority);
CREATE INDEX idx_incidents_assigned ON fraud_incidents(assigned_to);
CREATE INDEX idx_incidents_created_at ON fraud_incidents(created_at DESC);

-- 2. Create Incident Audit Trail Log
CREATE TABLE incident_audit_log (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL,
    actor_id UUID,
    actor_username VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    comment TEXT,
    previous_status VARCHAR(30),
    new_status VARCHAR(30),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_incident FOREIGN KEY (incident_id) REFERENCES fraud_incidents(id) ON DELETE CASCADE
);

CREATE INDEX idx_audit_incident_id ON incident_audit_log(incident_id);

-- 3. Create AI Incident Explanations Table
CREATE TABLE ai_explanations (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL UNIQUE,
    summary TEXT NOT NULL,
    factors TEXT,
    recommended_actions TEXT,
    confidence_level VARCHAR(20) NOT NULL,
    model_used VARCHAR(50) NOT NULL,
    is_fallback BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_explanation_incident FOREIGN KEY (incident_id) REFERENCES fraud_incidents(id) ON DELETE CASCADE
);
