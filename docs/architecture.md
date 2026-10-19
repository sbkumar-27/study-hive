# Architecture

## Components

- **Backend** (`backend/`): Spring Boot 4 REST API. Owns all business
  rules, authorization, and the only connection to MySQL.
- **Web client** (`web/`): React + TypeScript + Vite. Calls the backend
  over HTTP/JSON. Responsive for desktop and mobile browsers.
- **Desktop client** (`desktop/`): JavaFX + FXML + CSS. Calls the same
  backend over HTTP/JSON using Java's `HttpClient` + Jackson. No local
  database access.
- **Database**: MySQL 8, database `studyhive`. Schema in
  `database/schema.sql`.

## Hard rule: no client talks to MySQL directly

Both the web and desktop clients are pure HTTP clients of the Spring Boot
API. This keeps business rules and permission checks in one place, lets
both clients stay consistent by construction, and means the database can
be moved, migrated, or replaced without touching either client.

## Authentication

Stateless JWT bearer tokens, issued by `POST /api/v1/auth/login` and
`POST /api/v1/auth/register`, validated on every request by a Spring
Security filter. Both clients store the token (web: memory +
`localStorage` for session persistence; desktop: in a config file under
the user's profile) and send it as `Authorization: Bearer <token>`. This
approach (rather than server-side sessions) was chosen because it works
identically for a browser and a native desktop app without needing
cookie-jar/session-affinity handling in the JavaFX HTTP client.

Spring Security 7 (shipped with Boot 4) enables CSRF protection by
default even for non-browser-form endpoints. Since this API is stateless,
token-authenticated, and not cookie-based, CSRF protection is explicitly
disabled for `/api/v1/**` in `SecurityConfig` — there is no session
cookie for an attacker to ride on.

## File storage

Uploaded files are never trusted by name or path from the client. The
backend is the only thing that decides where a file lives:

- `storage.StorageProvider` is an interface (`store`, `retrieve`,
  `delete`, `exists`) with a `LocalFileSystemStorageProvider`
  implementation that writes under `STORAGE_ROOT` using a generated,
  collision-free storage key (not the original filename), so a cloud
  provider (e.g. S3) can be added later by implementing the same
  interface.
- Every uploaded file is hashed (SHA-256) and the file's declared
  MIME/extension is checked against an allow-list; size is capped by
  `MAX_UPLOAD_SIZE_MB`.
- A material's file history lives in `material_versions`: uploading a new
  version never overwrites or deletes the previous one.

## Request flow example: uploading a material

1. Client sends `multipart/form-data` to
   `POST /api/v1/materials` with a JWT.
2. `JwtAuthFilter` resolves the authenticated user.
3. `MaterialService` validates the community/subject relationship (a
   subject must belong to the same community as the material), checks
   the user is a member of that community (or that this is a Personal
   Vault upload with `communityId = null`), validates the file via
   `StorageProvider`, and persists `materials` + the first
   `material_versions` row in one transaction.
4. The response DTO never exposes the JPA entity directly — see
   `dto/` and `mapper/`.

## Why Maven multi-module (not separate repos)

`backend` and `desktop` are Maven modules under one root `pom.xml` so a
single `mvn -q -pl backend,desktop -am install` (or per-module build)
works from one clone, matching the single-repository branch plan in
section 3 of the master plan. `web` is intentionally **not** a Maven
module — it is an independent npm project, built and run with its own
tooling, and the two build systems are not meant to interleave.
