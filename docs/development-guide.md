# Development guide (Windows)

## Prerequisites

Checked in this environment on 2026-10-19:

| Tool | Found | Notes |
|---|---|---|
| Java | 26.0.1 | Backend compiles to Java 21 bytecode (`java.version` in root `pom.xml`) for broad library compatibility; runs fine on 26. |
| Maven | 3.10.0 | |
| Node | 24.19.0 | |
| npm | 12.0.2 | |
| MySQL | 8.0 (`MySQL80` service) | Workbench 8.0 CE also installed. |

## One-time setup

1. `copy .env.example .env` and fill in real values (DB password, a
   random `JWT_SECRET`, etc.). `.env` is git-ignored.
2. Apply the database schema — see [`database/README.md`](../database/README.md).

## Build order (matches the master plan's stage order)

1. **Database** — `database/schema.sql` (done; matches the live DB).
2. **Backend** — `cd backend && mvn clean verify`
3. **Storage** — part of the backend module (`storage/` package).
4. **Web** — `cd web && npm install && npm run build`
5. **Desktop** — `cd desktop && mvn clean verify`
6. **Integration/tests** — run all of the above together locally.

## Running everything locally

```bash
scripts\dev-backend.bat   REM Spring Boot on :8080
scripts\dev-web.bat       REM Vite dev server on :5173
scripts\dev-desktop.bat   REM JavaFX window, talks to :8080
```

Run the backend first; both clients need it.

## Running tests

```bash
cd backend && mvn test
cd ..\desktop && mvn test
cd ..\web && npm run build   REM no frontend test suite yet; this at least typechecks + bundles
```

## Status log

Updated as each stage completes with what was actually verified to run,
not what was merely written.

- **2026-10-19** — Foundation stage: repo structure, root/backend/desktop
  `pom.xml`, `database/schema.sql` (verified against the live `studyhive`
  DB — all 10 tables already matched; only the `popular_materials` view
  was added), docs skeletons, `.env.example`, `.gitignore`. Not yet built
  or run (no application code exists yet).
- **2026-10-19/20** — Backend stage complete: full Spring Boot 4 REST API
  (auth, communities, subjects, materials + versioning, favorites, views,
  question papers, reports) on `feature/backend`. `mvn test` passes (12
  tests: access-control unit tests + one MockMvc end-to-end test on H2).
  Additionally hand-verified the full golden path against the real local
  MySQL `studyhive` database with `scripts\dev-backend.bat` + curl:
  register/login, create a public community and a password-protected
  private one, join both (and confirmed a wrong/missing password is
  rejected), create a subject, upload a material and a question paper,
  search/filter, download, add a second file version (first version still
  retained), favorite/unfavorite, record a view, leave a community
  (blocked for the owner), submit and resolve a report, and confirmed a
  non-member gets 403 on a community material and on another user's
  Personal Vault item. See `backend/README.md` for the MySQL/JDBC/Spring
  Boot 4 quirks this surfaced (Jackson 3, `-parameters`, YEAR/TINYINT
  column mapping) and exactly how each was fixed.
- **2026-10-20** — Web stage complete on `feature/web`: full React +
  TypeScript + Vite client (auth, communities, subjects, materials +
  versions, vault, favorites, question papers, reports, profile).
  `npm run build` passes (typecheck + production bundle). Manually
  clicked through the entire app against the real backend + MySQL: register,
  login, logout, protected-route redirect, create a public and a
  password-protected private community, join by code (wrong code, wrong
  password, and correct password all behave correctly), add a subject,
  upload/search/view/download/favorite/edit a material, confirmed version
  history is append-only in the UI, and confirmed empty states. Along the
  way, found and fixed two real bugs: the material detail page's view
  count didn't refresh after recording a view (now invalidates the query),
  and a `.gitignore` bug (`storage/`/`uploads/` with no anchoring slash
  matched the backend's `storage` *source package* at any depth, silently
  excluding 4 files from every commit on `feature/backend` despite local
  tests passing) — fixed on every branch, see `backend/README.md`.
