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
| POST | `/auth/register` | none | ✅ | Create account. Body: `{username, email, password, displayName}`. Returns `{token, user}`. |
| POST | `/auth/login` | none | ✅ | Body: `{usernameOrEmail, password}`. Returns `{token, user}`. |
| GET | `/auth/me` | bearer | ✅ | Returns the current authenticated user. |

## `/users`

| Method | Path | Auth | Status | Description |
|---|---|---|---|---|
| GET | `/users/{id}` | bearer | planned | Public profile fields only. |

## `/communities`

| Method | Path | Auth | Status | Description |
|---|---|---|---|---|
| POST | `/communities` | bearer | ✅ | Create community; creator becomes `OWNER`. Body: `{name, description?, communityType?, privatePassword?}`. |
| GET | `/communities` | bearer | ✅ | List/search **public** communities (`?q=&page=&size=`). |
| GET | `/communities/mine` | bearer | ✅ | Communities the caller belongs to (public or private). |
| GET | `/communities/{id}` | bearer | ✅ | Community detail; 403 if private and neither a member nor a share-link guest. `guestAccess` is true for guests. |
| POST | `/communities/join` | bearer | ✅ | Body: `{joinCode, password?}`. `password` required only if the community set one. |
| POST | `/communities/{id}/leave` | bearer | ✅ | Owner cannot leave without transferring ownership first. |
| GET | `/communities/{id}/members` | bearer | ✅ | Members only. |
| GET | `/communities/link/{joinCode}` | bearer | ✅ | Share-link preview card (name, type, `requiresPassword`, `hasAccess`). Grants nothing. |
| POST | `/communities/link/{joinCode}/access` | bearer | ✅ | Body: `{password?}`. Grants **read-only guest access without joining**: view/download materials, subjects and question papers. Uploading, adding subjects and the member list stay members-only. Web share URL: `/c/{joinCode}`. |

## `/subjects`

| Method | Path | Auth | Status | Description |
|---|---|---|---|---|
| POST | `/communities/{communityId}/subjects` | bearer | ✅ | Members only. |
| GET | `/communities/{communityId}/subjects` | bearer | ✅ | Members only. |

## `/materials`

| Method | Path | Auth | Status | Description |
|---|---|---|---|---|
| POST | `/materials` | bearer | ✅ | `multipart/form-data`: part `file` + part `metadata` (JSON body of `CreateMaterialRequest`). Omit `communityId` for a Personal Vault upload. |
| GET | `/materials` | bearer | ✅ | Search/filter: `?q=&communityId=&subjectId=&type=&sort=newest\|popular&page=&size=`. |
| GET | `/materials/{id}` | bearer | ✅ | Authorization-checked detail. |
| PUT | `/materials/{id}` | bearer | ✅ | Metadata-only update; uploader only. |
| GET | `/materials/vault` | bearer | ✅ | Current user's Personal Vault. |
| GET | `/materials/{id}/download` | bearer | ✅ | Downloads the latest version. |

## `/materials/{id}/versions`

| Method | Path | Auth | Status | Description |
|---|---|---|---|---|
| POST | `/materials/{id}/versions` | bearer | ✅ | Upload a new version (uploader only; never deletes old ones). Parts: `file` + optional `versionNotes`. |
| GET | `/materials/{id}/versions` | bearer | ✅ | Version history, newest first. |
| GET | `/materials/{id}/versions/{versionId}/download` | bearer | ✅ | Download a specific (including historical) version. |

## `/materials/{id}/favorite`

| Method | Path | Auth | Status | Description |
|---|---|---|---|---|
| POST | `/materials/{id}/favorite` | bearer | ✅ | Add to favorites. |
| DELETE | `/materials/{id}/favorite` | bearer | ✅ | Remove. |

## `/materials/{id}/view`

| Method | Path | Auth | Status | Description |
|---|---|---|---|---|
| POST | `/materials/{id}/view` | bearer | ✅ | Record a view (deduped per user per 30 minutes; see `docs/database-design.md`). Returns `{viewCount}`. |

## `/question-papers`

| Method | Path | Auth | Status | Description |
|---|---|---|---|---|
| GET | `/question-papers` | bearer | ✅ | Filter by `subjectId`, `examYear`, `examType`, `semester`. |

## `/reports`

| Method | Path | Auth | Status | Description |
|---|---|---|---|---|
| POST | `/reports` | bearer | ✅ | Body: `{materialId, reason}`. |
| GET | `/reports` | bearer (MOD/ADMIN) | ✅ | List pending reports: all for `ADMIN`, only the caller's moderated communities otherwise. |
| PUT | `/reports/{id}` | bearer (MOD/ADMIN) | ✅ | Resolve: `{status: REVIEWED\|DISMISSED}`. |
