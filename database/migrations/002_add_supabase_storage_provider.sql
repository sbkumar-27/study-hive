-- Additive migration: allow FIREBASE and SUPABASE alongside the existing
-- LOCAL/CATALYST storage providers on material_versions.storage_provider.
-- Widening an ENUM like this does not touch existing row data -- all
-- current LOCAL rows (and any CATALYST/FIREBASE rows) are unaffected.
--
-- This matches the live database's current column definition exactly
-- (ENUM('LOCAL','CATALYST','FIREBASE','SUPABASE'), applied manually
-- outside this migration file). Kept here, NOT executed, so schema.sql and
-- the migrations/ history stay the source of truth for what a fresh
-- install needs to reach the same state:
--   "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p studyhive < database/migrations/002_add_supabase_storage_provider.sql

ALTER TABLE material_versions
  MODIFY storage_provider ENUM('LOCAL', 'CATALYST', 'FIREBASE', 'SUPABASE') NOT NULL DEFAULT 'LOCAL';
