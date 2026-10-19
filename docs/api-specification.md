# API specification

Base path: `/api/v1`. All request/response bodies are JSON (file upload
endpoints use `multipart/form-data`). Authenticated endpoints require
`Authorization: Bearer <jwt>`.

## Conventions

- Error responses: `{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "...", "path": "..." }`
  (see `exception/GlobalExceptionHandler`).
- Pagination: `?page=0&size=20` query params; responses wrap results as
  `{ "content": [...], "page": 0, "size": 20, "totalElements": N, "totalPages": N }`.
- IDs are `BIGINT UNSIGNED` in MySQL, serialized as JSON numbers.
- Timestamps are ISO-8601 UTC strings.

Endpoints are documented here as each is implemented; status column
tracks progress.

## `/auth`

| Method | Path | Auth | Status | Description |
|---|---|---|---|---|
| POST | `/auth/register` | none | planned | Create account. Body: `{username, email, password, displayName}`. Returns `{token, user}`. |
| POST | `/auth/login` | none | planned | Body: `{usernameOrEmail, password}`. Returns `{token, user}`. |
| GET | `/auth/me` | bearer | planned | Returns the current authenticated user. |

## `/users`

| Method | Path | Auth | Status | Description |
|---|---|---|---|---|
| GET | `/users/{id}` | bearer | planned | Public profile fields only. |

## `/communities`

| Method | Path | Auth | Status | Description |
|---|---|---|---|---|
| POST | `/communities` | bearer | planned | Create community; creator becomes `OWNER`. |
| GET | `/communities` | bearer | planned | List/search public communities (`?q=`). |
| GET | `/communities/{id}` | bearer | planned | Community detail (members-only fields hidden unless a member). |
| POST | `/communities/join` | bearer | planned | Body: `{joinCode[, password]}`. |
| POST | `/communities/{id}/leave` | bearer | planned | Owner cannot leave without transferring ownership. |
| GET | `/communities/{id}/members` | bearer | planned | Members only. |

## `/subjects`

| Method | Path | Auth | Status | Description |
|---|---|---|---|---|
| POST | `/communities/{communityId}/subjects` | bearer | planned | Members only. |
| GET | `/communities/{communityId}/subjects` | bearer | planned | Members only. |

## `/materials`

| Method | Path | Auth | Status | Description |
|---|---|---|---|---|
| POST | `/materials` | bearer | planned | `multipart/form-data`: metadata fields + `file`. |
| GET | `/materials` | bearer | planned | Search/filter: `?q=&subjectId=&communityId=&type=&sort=newest|popular`. |
| GET | `/materials/{id}` | bearer | planned | Authorization-checked detail. |
| PUT | `/materials/{id}` | bearer | planned | Metadata-only update. |
| GET | `/materials/vault` | bearer | planned | Current user's Personal Vault. |

## `/materials/{id}/versions`

| Method | Path | Auth | Status | Description |
|---|---|---|---|---|
| POST | `/materials/{id}/versions` | bearer | planned | Upload a new version (does not delete old ones). |
| GET | `/materials/{id}/versions` | bearer | planned | Version history. |
| GET | `/materials/{id}/versions/{versionId}/download` | bearer | planned | Download a specific (including historical) version. |

## `/materials/{id}/favorite`

| Method | Path | Auth | Status | Description |
|---|---|---|---|---|
| POST | `/materials/{id}/favorite` | bearer | planned | Add to Personal Vault favorites. |
| DELETE | `/materials/{id}/favorite` | bearer | planned | Remove. |

## `/materials/{id}/view`

| Method | Path | Auth | Status | Description |
|---|---|---|---|---|
| POST | `/materials/{id}/view` | bearer | planned | Record a view (deduped per `docs/database-design.md`). |

## `/question-papers`

| Method | Path | Auth | Status | Description |
|---|---|---|---|---|
| GET | `/question-papers` | bearer | planned | Filter by `subjectId`, `examYear`, `examType`, `semester`. |

## `/reports`

| Method | Path | Auth | Status | Description |
|---|---|---|---|---|
| POST | `/reports` | bearer | planned | Body: `{materialId, reason}`. |
| GET | `/reports` | bearer (MOD/ADMIN) | planned | List pending reports. |
| PUT | `/reports/{id}` | bearer (MOD/ADMIN) | planned | Resolve: `{status: REVIEWED|DISMISSED}`. |
