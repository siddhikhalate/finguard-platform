-- ==============================================================================
-- FinGuard Auth Service - Database Migration V4
-- Drop plaintext token storage and enforce strict token_hash column
-- ==============================================================================

-- 1. Drop old plaintext token index
DROP INDEX IF EXISTS idx_refresh_tokens_token;

-- 2. Backfill any rows that may have null token_hash
UPDATE refresh_tokens 
SET token_hash = encode(sha256(token::bytea), 'hex') 
WHERE token_hash IS NULL AND token IS NOT NULL;

-- 3. Delete any dangling invalid rows missing token_hash
DELETE FROM refresh_tokens WHERE token_hash IS NULL;

-- 4. Drop the plaintext token column completely
ALTER TABLE refresh_tokens DROP COLUMN IF EXISTS token;

-- 5. Set token_hash as NOT NULL and UNIQUE
ALTER TABLE refresh_tokens ALTER COLUMN token_hash SET NOT NULL;

DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_refresh_tokens_token_hash'
    ) THEN
        ALTER TABLE refresh_tokens ADD CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash);
    END IF;
END $$;
