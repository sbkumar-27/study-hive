-- Optional sample data for local development/manual testing.
-- Safe to run multiple times (uses INSERT IGNORE). Do not run against a
-- database you care about keeping clean -- this is for dev only.
--
-- Sample users are NOT inserted here because a valid password_hash must be
-- produced by the backend's BCryptPasswordEncoder. Instead, once the
-- backend is running, create them via the real registration endpoint:
--
--   POST /api/v1/auth/register
--   { "username": "alice", "email": "alice@example.com",
--     "password": "Password123!", "displayName": "Alice Example" }
--
-- The community/subject rows below reference user_id 1 and 2, so register
-- "alice" first (becomes user_id 1) and "bob" second (becomes user_id 2)
-- before running the rest of this script.

USE studyhive;

INSERT IGNORE INTO communities (community_id, name, description, community_type, join_code, owner_id)
VALUES
    (1, 'Computer Science 2026', 'CS batch community for shared notes', 'PUBLIC', 'CS2026DEMO', 1);

INSERT IGNORE INTO community_members (community_id, user_id, member_role)
VALUES
    (1, 1, 'OWNER'),
    (1, 2, 'MEMBER');

INSERT IGNORE INTO subjects (subject_id, community_id, subject_code, subject_name)
VALUES
    (1, 1, 'CS301', 'Data Structures and Algorithms'),
    (2, 1, 'CS302', 'Database Management Systems');
