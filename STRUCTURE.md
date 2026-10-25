# StudyHive — how this repo is put together

This is the "explain it to a friend" version of the project layout. If you already know
Spring Boot/React/JavaFX, read [`docs/architecture.md`](docs/architecture.md) instead for the
deeper version — this file is meant to make sense even if you don't.

## The six folders at the root

- **`backend/`** — the brain. A Spring Boot (Java) program that is the *only* thing allowed to
  talk to the database. It exposes a web API (a set of URLs like `/api/v1/communities`) that
  anything else — the website, the desktop app — calls to read or change data.
- **`web/`** — the website. A React app (what runs in your browser) that calls the backend's API
  and draws the screens you actually see and click on.
- **`desktop/`** — a second, optional front end: a JavaFX desktop program that does the same job
  as the website, just as a Windows app instead of a browser tab. It also only talks to the
  backend's API — never to the database directly.
- **`database/`** — the MySQL table definitions (`schema.sql`) and any changes made to them over
  time (`migrations/`), plus some sample data (`seed.sql`) for trying the app out locally.
- **`docs/`** — longer written explanations: architecture decisions, the full API reference, how
  the database is laid out, and a day-to-day setup guide.
- **`scripts/`** — small `.bat` files that start the backend/web/desktop apps on Windows without
  you having to remember the exact command each time.

These six names are fixed on purpose: Render (where the backend runs in production) is told to
build from `backend/Dockerfile`, and Vercel (where the website runs in production) is told to
build from the `web/` folder. Renaming either folder would break the live site, so only what's
*inside* them got reorganized.

## What's inside `backend/` and `web/` now

Both used to be organized by *technical layer* — all the database-access code in one folder, all
the web-endpoint code in another, all the little response-shape classes in a third, and so on.
To look at "everything about communities" you had to open five different folders.

Both are now organized by *feature* instead — everything about one part of the app lives
together:

- `auth` — registering and logging in.
- `communities` — creating/joining communities, subjects inside them, and the share-link
  (guest) feature.
- `materials` — uploaded files, their versions, favorites, view counts, and reports.
- `questionpapers` — question papers (a specialized kind of material).
- `common` (backend only) / `shared` (web only) — the plumbing every feature needs: security,
  file storage, error handling, shared UI pieces, and so on.

## How a request travels, start to finish

Say you open a community page in your browser. Here's the whole round trip:

```
 Your phone/PC browser
        │  1. loads the page
        ▼
 React app (hosted on Vercel)
        │  2. calls https://<backend>/api/v1/communities/42
        ▼
 Spring Boot API (hosted on Render)
        │  3. checks your login token, then asks the database layer
        ▼
 Hibernate / JPA  (translates Java objects <-> SQL)
        │  4. runs a SQL query
        ▼
 MySQL database (hosted on Aiven)
        │  5. returns the rows
        ▼
 Spring Boot API
        │  6. turns the rows into JSON, sends it back
        ▼
 React app
        │  7. draws the community page with that data
        ▼
 Your browser
```

One detail worth knowing: the actual **file bytes** of an uploaded PDF/DOCX/etc. never go through
MySQL — they go to **Supabase Storage** instead (a separate file-storage service). The database
only stores a *pointer* to where the file lives. So step 3 above, for a file download, actually
asks Supabase for the bytes instead of (or in addition to) asking MySQL for the file's metadata.

```
 Browser  ──requests a file──>  Spring Boot API  ──checks you're allowed──>  Supabase Storage
                                        │                                           │
                                        └──────────────── streams the file back ────┘
```

The JavaFX desktop app does the exact same round trip as the browser — it just draws its own
screens instead of HTML, and talks to the same `/api/v1/...` web address.
