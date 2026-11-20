-- ==============================================================================
-- FinGuard Auth Service - Database Migration V3
-- Remove hardcoded shared seed users from core production migrations (Phase 1.6)
-- Dev seed users are provisioned exclusively via the 'dev' Spring profile.
-- ==============================================================================

DELETE FROM user_roles WHERE user_id IN (
    'a0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000003'
);

DELETE FROM users WHERE id IN (
    'a0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000003'
);
