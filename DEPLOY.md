# Deploying StudyHive

Backend (Spring Boot) → **Render**, as a Docker web service.
Frontend (React/Vite) → **Vercel**.
Database → **Aiven** (managed MySQL, already provisioned).
File storage → **Supabase Storage** (required in production — Render's
filesystem is not persistent across deploys, so `STORAGE_PROVIDER=local`
would silently lose every uploaded file on the next deploy or restart).

## 1. Render environment variables (backend)

Set these in the Render dashboard under the web service's **Environment** tab.
Never put real values in a file that gets committed — this list is names and
purposes only.

| Variable | Purpose |
|---|---|
| `DB_HOST` | Aiven MySQL host |
| `DB_PORT` | Aiven MySQL port (Aiven typically uses a non-default port, not 3306) |
| `DB_NAME` | Database name (`studyhive`) |
| `DB_USERNAME` | Aiven MySQL username |
| `DB_PASSWORD` | Aiven MySQL password |
| `DB_SSL_MODE` | Leave as `REQUIRED` (the default) — Aiven mandates TLS |
| `JWT_SECRET` | Long random secret used to sign auth tokens — generate a fresh one for production, don't reuse the local dev value |
| `JWT_EXPIRATION_MS` | Token lifetime in ms (optional — defaults to 86400000 / 24h) |
| `STORAGE_PROVIDER` | Must be `supabase` in production |
| `MAX_UPLOAD_SIZE_MB` | Optional — defaults to 25 |
| `SUPABASE_URL` | The Supabase project's API URL |
| `SUPABASE_SERVICE_KEY` | Supabase service-role key (not the anon key — it needs write access) |
| `SUPABASE_BUCKET` | The Storage bucket materials are uploaded to |
| `WEB_APP_ORIGIN` | The live Vercel URL (e.g. `https://studyhive.vercel.app`), no trailing slash — CORS only allows this one origin |

Do **not** set `PORT` — Render injects it automatically and the app already
reads it (`server.port: ${PORT:${SERVER_PORT:8080}}` in `application.yml`).

`CATALYST_*` variables are not needed in production; the app starts fine
without them (every `CATALYST_*` reference has an empty default).

## 2. Render service setup (backend)

1. New → Web Service → connect this GitHub repo.
2. Runtime: **Docker**.
3. Dockerfile path: `backend/Dockerfile`.
4. Docker build context: repository root (the Dockerfile needs the root `pom.xml` — this is a Maven multi-module reactor).
5. Add every variable from the table above under Environment.
6. Deploy. Render builds the multi-stage Dockerfile (Maven build → JRE runtime) and runs `java -jar app.jar`, bound to Render's `$PORT`.
7. Once live, note the service URL (e.g. `https://studyhive-backend.onrender.com`) — it's needed for Vercel's `VITE_API_BASE_URL`.

Because `spring.jpa.hibernate.ddl-auto` is `validate` (not `update` or
`create-drop`), the app will refuse to start if the Aiven schema doesn't
already match the entities — it never creates, drops, or alters tables
itself. Apply `database/schema.sql` (and any pending files under
`database/migrations/`) to the Aiven database yourself before the first
deploy.

## 3. Vercel environment variables (frontend)

| Variable | Purpose |
|---|---|
| `VITE_API_BASE_URL` | The Render backend's origin only — e.g. `https://studyhive-backend.onrender.com` (no trailing slash, no `/api/v1`) |

## 4. Vercel project setup (frontend)

1. New Project → import this repo.
2. Root Directory: `web`.
3. Framework preset: Vite (auto-detected).
4. Build command / output: defaults (`npm run build`, `dist`) are fine.
5. Add `VITE_API_BASE_URL` under Environment Variables.
6. Deploy. `web/vercel.json` already rewrites every path to `/index.html`, so refreshing a client-side route like `/materials` or `/communities/42` works instead of 404ing.
7. Once live, set that Vercel URL as `WEB_APP_ORIGIN` on Render (step 1) and redeploy the backend so CORS allows it.

## 5. Order of operations (first deploy)

1. Apply `database/schema.sql` to the Aiven database (and `database/migrations/002_add_supabase_storage_provider.sql` if the live schema doesn't already allow `SUPABASE` as a `storage_provider` value — check first, don't assume).
2. Create the Supabase Storage bucket referenced by `SUPABASE_BUCKET`.
3. Deploy the backend on Render with `STORAGE_PROVIDER=supabase` and a placeholder `WEB_APP_ORIGIN` (you don't have the Vercel URL yet).
4. Deploy the frontend on Vercel with `VITE_API_BASE_URL` set to the Render URL from step 3.
5. Update `WEB_APP_ORIGIN` on Render to the real Vercel URL from step 4, redeploy the backend.
6. If migrating existing LOCAL materials to Supabase, see `backend/src/main/java/com/studyhive/backend/migration/MigrateLocalToSupabase.java` — review its dry-run output before ever running it with `--execute`.
