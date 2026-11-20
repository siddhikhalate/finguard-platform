-- ==============================================================================
-- FinGuard Auth Service - Database Migration V2
-- Security Hardening: Account Lockout & Refresh Token Hashing (Phase 1.7)
-- ==============================================================================

-- 1. Add failed login attempts counter and lockout timestamp to users
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS failed_login_attempts INT NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP WITH TIME ZONE NULL;

-- 2. Add SHA-256 token hash column to refresh_tokens
ALTER TABLE refresh_tokens
ADD COLUMN IF NOT EXISTS token_hash VARCHAR(64) NULL;

-- 3. Populate existing rows with SHA-256 hashes if any exist
UPDATE refresh_tokens 
SET token_hash = encode(sha256(token::bytea), 'hex') 
WHERE token_hash IS NULL;

-- 4. Create index on token_hash for high-speed constant-time lookup
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
