-- Additive migration: a short "pinned note" a community's owner can set,
-- shown at the top of the community's page (e.g. "Midterm moved to the 14th").
-- NULL/blank clears it. Nothing else about the community changes.
--
-- Run ONCE on every database the backend uses (local AND Aiven), e.g.:
--   mysql -u root -p studyhive < database/migrations/004_add_community_pinned_note.sql
-- The backend uses ddl-auto=validate, so it will refuse to start until
-- this column exists. Unlike migration 003's CREATE TABLE IF NOT EXISTS,
-- real MySQL (unlike MariaDB) has no ADD COLUMN IF NOT EXISTS, so this
-- is plain and NOT safe to run twice -- running it again on a database
-- that already has the column fails with "Duplicate column name".

ALTER TABLE communities ADD COLUMN pinned_note TEXT NULL;
