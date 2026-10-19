# Database design

Full DDL: [`database/schema.sql`](../database/schema.sql). This document
explains the *why* behind choices that aren't obvious from the DDL alone.

## Entity overview

```
users ──owns──> communities ──has──> subjects ──has──> materials ──has──> material_versions
  │                  │                                    │
  │                  └──has (M:N via community_members)   ├──1:1──> question_papers (if QUESTION_PAPER)
  │                                                        ├──has──> material_views
  └──favorites (M:N via material_favorites)                └──has──> material_reports
```

## Key invariants (enforced in the backend, not the DB)

| Rule | Where enforced |
|---|---|
| Community owner gets an `OWNER` row in `community_members` | `CommunityService.createCommunity` (same transaction) |
| A material's `subject_id` must belong to the same `community_id` | `MaterialService` validation before insert/update |
| Personal Vault materials: `community_id`/`subject_id` NULL, `visibility = PRIVATE`, only visible to `uploaded_by` | `MaterialService` + repository query predicates; checked on every read, not just write |
| Only community members can see `COMMUNITY`-visibility materials of a community; private communities require membership (join code or, for password-protected private communities, the join flow) | `MaterialService` / `CommunityService` authorization checks on every read endpoint |
| Updating a material creates a new `material_versions` row; old versions are never updated/deleted | `MaterialVersionService.addVersion` only ever inserts |
| Upload validation: filename/path never trusted, extension + size checked, checksum stored | `StorageProvider` + `MaterialUploadValidator` |
| Moderation actions require `ADMIN` or the community's `MODERATOR`/`OWNER` role | method-level `@PreAuthorize` / explicit role checks in `ReportService` |

Hiding a UI button is never treated as authorization — every one of the
rules above is re-checked server-side on each request.

## What counts as a "view"

To avoid trivially inflating popularity by refreshing a page, a
`material_views` row is only inserted:

- at most once per `(material_id, user_id)` pair per rolling 30-minute
  window for authenticated users, and
- at most once per `(material_id, client_fingerprint)` per rolling
  30-minute window for anonymous access where materials are public,

enforced in `MaterialViewService` by checking the most recent
`material_views` row for that pair before inserting a new one, rather
than by a DB constraint (the rule may need tuning, and belongs in
application logic per the architecture rule in section 2 of the master
plan).

## Why `material_id` has nullable `community_id`/`subject_id`

This is what makes the **Personal Vault** possible without a separate
table: a vault item is just a `materials` row with no community/subject
and `visibility = PRIVATE`. The alternative (a separate `vault_items`
table) would duplicate most of `materials`' columns and double the
places version history / favorites / reports have to join against.

## Popularity

The `popular_materials` view aggregates `COUNT(material_views.view_id)`
per material. The backend's `/materials?sort=popular` endpoint queries
this view rather than recomputing the count per request.
