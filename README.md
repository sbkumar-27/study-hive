# StudyHive

## Live demo

- Web app: _add the deployed Vercel URL here once live, e.g. `https://studyhive.vercel.app`_
- Backend API: _add the deployed Render URL here once live, e.g. `https://studyhive-backend.onrender.com/api/v1`_

See [`DEPLOY.md`](DEPLOY.md) for how this is deployed (Render + Vercel + Aiven MySQL + Supabase Storage).

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

## Run StudyHive on your own computer

A from-scratch walkthrough for someone who has never seen this project
before. It gets you the web app + backend running locally with **no cloud
accounts needed** — local storage and a local MySQL database are the
defaults.

### 1. Install the prerequisites

| Tool | Version | Download |
|---|---|---|
| **Git** | any recent version | https://git-scm.com/downloads |
| **JDK** | 21 (Temurin) | https://adoptium.net/temurin/releases/?version=21 |
| **Maven** | 3.9+ | https://maven.apache.org/download.cgi (see their install guide for adding it to your PATH) |
| **Node.js** | 20+ (includes npm) | https://nodejs.org/en/download |
| **MySQL** | 8.x Server | https://dev.mysql.com/downloads/mysql/ (Windows: the installer offers MySQL Workbench too, which is handy but optional) |

After installing, open a **new** terminal (so PATH changes take effect) and sanity-check each one:

```bash
git --version
java -version     # should report version 21
mvn -version
node -version
npm -version
mysql --version
```

### 2. Clone the repository

```bash
git clone https://github.com/sbkumar/study-hive.git
cd study-hive
```

### 3. Create the local database

Make sure MySQL is running, then apply the schema (safe to re-run — every
table uses `CREATE TABLE IF NOT EXISTS`, and the script creates the
`studyhive` database itself, so there's no separate "create database" step):

```bash
mysql -u root -p < database/schema.sql
```

Windows note: if `mysql` isn't on your PATH, use the full path instead, e.g.:
```cmd
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p < database/schema.sql
```

(Optional) Load sample data — see [`database/README.md`](database/README.md) for the exact steps, including registering the sample users first.

### 4. Configure environment variables

```bash
copy .env.example .env        REM Windows
cp .env.example .env          # Mac/Linux
```

Open `.env` and fill in what you need. Everything below already has a
sensible local default in `.env.example` — for a first run on your own
machine, the only thing you'll likely change is `DB_PASSWORD` (and
`DB_USERNAME` if you're not using `root`).

| Variable | What it's for | Local default |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | MySQL connection | `localhost` / `3306` / `studyhive` |
| `DB_USERNAME` / `DB_PASSWORD` | MySQL credentials | `root` / set this to your actual local MySQL root password |
| `DB_SSL_MODE` | TLS mode for the DB connection | `REQUIRED` works everywhere; only change it if your local MySQL has no TLS configured at all, in which case use `DISABLED` |
| `JWT_SECRET` | Signs login tokens | any long random string is fine for local dev |
| `JWT_EXPIRATION_MS` | How long a login session lasts | `86400000` (24h) |
| `STORAGE_PROVIDER` | Where uploaded files go | `local` — stores files on disk under `STORAGE_ROOT`, no cloud account needed |
| `STORAGE_ROOT` | Local folder for uploaded files | `./storage` |
| `MAX_UPLOAD_SIZE_MB` | Upload size limit | `25` |
| `CATALYST_*` / `SUPABASE_*` | Only needed if you set `STORAGE_PROVIDER` to `catalyst` or `supabase` | leave blank for local |
| `WEB_APP_ORIGIN` | Which frontend origin the backend accepts requests from (CORS) | `http://localhost:5173` |
| `SERVER_PORT` | Backend port | `8080` |

### 5. Start the backend

Windows (Command Prompt):
```cmd
scripts\dev-backend.bat
```

Mac/Linux:
```bash
export $(grep -v '^#' .env | xargs)
cd backend && mvn spring-boot:run
```

Wait for `Started StudyHiveApplication` in the log. The API is now at `http://localhost:8080/api/v1`.

### 6. Start the frontend

In a **second** terminal:

Windows:
```cmd
scripts\dev-web.bat
```

Mac/Linux:
```bash
cd web && npm install && npm run dev
```

### 7. Open the app

Go to **http://localhost:5173** in your browser and register a new account.

### 8. (Optional) Desktop client

```cmd
scripts\dev-desktop.bat
```

### Common errors and fixes

- **`Port 8080 was already in use`** — something else is already running on that port.
  - Windows: `netstat -ano | findstr :8080`, then `taskkill /PID <pid> /F`.
  - Mac/Linux: `lsof -i :8080`, then `kill <pid>`.
  - Or just set a different `SERVER_PORT` in `.env`.
- **`Access denied for user 'root'@'localhost'`** (MySQL) — `DB_USERNAME`/`DB_PASSWORD` in `.env` don't match a real MySQL account. Confirm you can log in manually with `mysql -u root -p` using the same password, and that MySQL's service is actually running.
- **`'mvn' is not recognized as an internal or external command`** — Maven's `bin` folder isn't on your PATH. Re-check the install guide linked above; after editing PATH you must open a **new** terminal window.
- **Frontend loads but every request fails / blank data** — the backend isn't running, or `.env`'s `DB_*` values are wrong so it failed to start. Check the backend terminal for errors first.
- **`mysql: command not found`** — MySQL's `bin` directory isn't on your PATH either; use the full path to `mysql`/`mysql.exe` as shown in step 3, or add it to PATH.

## Status

All stages are implemented and merged into `main`: foundation, database,
backend, web client, desktop client, and integration. The full root
build (`mvn clean verify` at the repo root + `npm run build` in `web/`)
passes cleanly. Backend and web were each verified with automated tests
*and* a full manual walkthrough/click-through against the real backend +
MySQL. Desktop compiles and launches cleanly against the real backend
(confirmed via the OS process list), but this environment has no GUI
automation tools for desktop apps, so its screens have not been clicked
through the way the backend and web client were -- see
`desktop/README.md` for exactly what that means and what to check
manually. See `docs/development-guide.md` for the detailed status log,
and `backend/README.md` / `web/README.md` / `desktop/README.md` for
what was tested and how in each.

## Security notes

- Passwords are hashed with BCrypt; plain-text passwords are never stored.
- `.env`, uploaded files, and build output are git-ignored — see
  `.gitignore`. Only `.env.example` (placeholders) is committed.
- Authorization is enforced in the backend on every endpoint, not just
  hidden in the UI.
