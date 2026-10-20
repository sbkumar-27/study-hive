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
cd ..\web && npm test
```

## Status log

Updated as each stage completes with what was actually verified to run,
not what was merely written.

- **2026-10-19** — Foundation stage: repo structure, root/backend/desktop
  `pom.xml`, `database/schema.sql` (verified against the live `studyhive`
  DB — all 10 tables already matched; only the `popular_materials` view
  was added), docs skeletons, `.env.example`, `.gitignore`. Not yet built
  or run (no application code exists yet).
- **2026-10-20** — Desktop stage complete on `feature/desktop`: full
  JavaFX 21 client (login/register, communities, subjects, materials +
  versions, vault, favorites, question papers, reports, profile), reusing
  the same REST API contract as the web client. `mvn compile` passes (83
  classes). Launched with `mvn javafx:run` against the real backend +
  MySQL: starts with no exceptions, JavaFX native graphics load correctly,
  window opens with the right title. This session has no GUI automation
  tools for desktop apps (only the browser), so unlike the backend and
  web client, the screens were not clicked through end-to-end — see
  `desktop/README.md` for exactly what was and wasn't verified.
