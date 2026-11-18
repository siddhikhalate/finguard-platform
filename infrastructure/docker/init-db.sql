-- FinGuard Multi-Service Database Initialization Script
-- Executed on initial PostgreSQL container startup

CREATE DATABASE auth_db;
CREATE DATABASE txn_db;
CREATE DATABASE fraud_db;
CREATE DATABASE incident_db;

-- Grant all privileges to finguard user
GRANT ALL PRIVILEGES ON DATABASE auth_db TO finguard;
GRANT ALL PRIVILEGES ON DATABASE txn_db TO finguard;
GRANT ALL PRIVILEGES ON DATABASE fraud_db TO finguard;
GRANT ALL PRIVILEGES ON DATABASE incident_db TO finguard;
