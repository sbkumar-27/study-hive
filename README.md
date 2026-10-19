# StudyHive

A community-based study-material sharing platform for college students.
Students register, create or join communities with a unique join code,
organize materials by subject, upload/download notes and question papers,
keep a Personal Vault of private materials, and track popularity by views.

The same Spring Boot REST API and MySQL database back both a React web
app and a JavaFX desktop app. Neither client talks to MySQL directly.

```
Browser  ──┐
           ├─> React (Vite, :5173)  ──┐
JavaFX   ──┘                          ├─> Spring Boot API (:8080) ──> MySQL 8 (studyhive)
Desktop  ─────────────────────────────┘
```

## Repository layout

| Path | Contents |
|---|---|
| `backend/` | Spring Boot REST API (Java, Maven) |
| `desktop/` | JavaFX desktop client (Java, Maven) |
| `web/` | React + TypeScript web client (Vite) |
| `database/` | Schema, seed data, DB setup notes |
| `docs/` | Architecture, API spec, DB design, dev guide |
| `scripts/` | Windows `.bat` launchers for each app |

See [`docs/architecture.md`](docs/architecture.md) for the full
architecture rationale and [`docs/development-guide.md`](docs/development-guide.md)
for day-to-day setup commands.

## Prerequisites (Windows)

- Java JDK 17+ (developed against JDK 21 target / tested on 26)
- Maven 3.9+
- Node.js 20+ and npm
- MySQL 8 Server + Workbench (or just the `mysql` CLI)

## Quick start

1. **Database** — see [`database/README.md`](database/README.md).
2. **Backend**
   ```bash
   copy .env.example .env
   REM edit .env with your real DB password and a JWT secret
   scripts\dev-backend.bat
   ```
   API runs at `http://localhost:8080/api/v1`.
3. **Web**
   ```bash
   scripts\dev-web.bat
   ```
   Opens at `http://localhost:5173`.
4. **Desktop**
   ```bash
   scripts\dev-desktop.bat
   ```

## Status

This project is being built in stages (database → backend → storage → web
→ desktop → integration → tests). Current stage: **foundation** complete,
**backend** in progress. See `docs/development-guide.md` for the detailed
build order and what is verified to work so far.

## Security notes

- Passwords are hashed with BCrypt; plain-text passwords are never stored.
- `.env`, uploaded files, and build output are git-ignored — see
  `.gitignore`. Only `.env.example` (placeholders) is committed.
- Authorization is enforced in the backend on every endpoint, not just
  hidden in the UI.
