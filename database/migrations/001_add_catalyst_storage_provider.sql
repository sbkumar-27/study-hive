-- Additive migration: allow CATALYST alongside the existing LOCAL storage
-- provider on material_versions.storage_provider. Widening an ENUM like this
-- does not touch existing row data -- all current LOCAL rows are unaffected.
--
-- NOT executed automatically. Run manually only when Catalyst storage is
-- actually being enabled:
--   "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p studyhive < database/migrations/001_add_catalyst_storage_provider.sql

ALTER TABLE material_versions
  MODIFY storage_provider ENUM('LOCAL', 'CATALYST') NOT NULL DEFAULT 'LOCAL';
