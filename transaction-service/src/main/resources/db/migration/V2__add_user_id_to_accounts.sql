-- ==============================================================================
-- FinGuard Transaction Service - Database Migration V2
-- Add user_id column for Object-Level Access Control (OLAC)
-- ==============================================================================

ALTER TABLE accounts ADD COLUMN user_id UUID;
CREATE INDEX idx_accounts_user_id ON accounts(user_id);

-- Link Seeded Demo Accounts to Default Seed Users:
-- Standard User ID: a0000000-0000-0000-0000-000000000003 (user@finguard.internal)
UPDATE accounts 
SET user_id = 'a0000000-0000-0000-0000-000000000003' 
WHERE account_number IN ('ACC-100201', 'ACC-100202');

-- Corporate Account (Apex Corp Treasury) linked to separate user
UPDATE accounts 
SET user_id = 'a0000000-0000-0000-0000-000000000004' 
WHERE account_number = 'ACC-100203';
