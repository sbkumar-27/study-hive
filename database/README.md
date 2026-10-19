# StudyHive database

MySQL 8, database name `studyhive`. The ten application tables plus the
`popular_materials` view are defined in [`schema.sql`](schema.sql).

## Setup (Windows)

1. Make sure the `MySQL80` service is running (Services app, or
   `net start MySQL80`).
2. Apply the schema (safe to re-run; every table uses
   `CREATE TABLE IF NOT EXISTS`, so existing data is never dropped):

   ```bash
   "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p < database/schema.sql
   ```

3. Start the backend once (see [backend README](../backend/README.md) once
   it exists / the root `README.md`), then register the two sample users
   through the API before loading sample data:

   ```bash
   "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p studyhive < database/seed.sql
   ```

## Design notes

- `materials.community_id` / `materials.subject_id` are nullable: a
  **Personal Vault** material has both set to `NULL` and `visibility =
  'PRIVATE'`, and is only ever visible to `uploaded_by`.
- `material_versions` is append-only. Updating a material's file inserts a
  new row with an incremented `version_number`; existing rows are never
  updated or deleted. `storage_key` is unique per provider so two versions
  never collide on disk.
- `question_papers` is a 1:1 extension of `materials` (via
  `material_id UNIQUE`) used only when `material_type = 'QUESTION_PAPER'`.
- Popularity is derived from `material_views`, exposed as the
  `popular_materials` view (one row per material with a `view_count`). The
  backend decides what counts as a view (see
  `docs/database-design.md`) so refreshes don't trivially inflate counts.
- `community_members` carries `OWNER` / `MODERATOR` / `MEMBER`. The backend
  must insert an `OWNER` row whenever a community is created — this is an
  application-level invariant, not a DB trigger, to keep logic in one
  place (the backend).
