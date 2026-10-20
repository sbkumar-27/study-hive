# StudyHive web client

React 19 + TypeScript + Vite. Talks only to the Spring Boot backend's REST
API (see [`../docs/api-specification.md`](../docs/api-specification.md)) --
it never touches MySQL directly.

## Run locally (Windows)

1. Start the backend first (see [`../backend/README.md`](../backend/README.md)).
2. From the repo root: `scripts\dev-web.bat` (runs `npm install` on first
   run, then `npm run dev`).

Opens at `http://localhost:5173`. The Vite dev server proxies `/api/**`
to `http://localhost:8080` (see `vite.config.ts`), so the app calls
relative paths and never needs CORS configuration in dev.

## Stack

- **Routing:** `react-router-dom`, with a single `ProtectedRoute` guard
  wrapping everything except `/login` and `/register`.
- **Data fetching:** `@tanstack/react-query` for caching, loading, and
  error state -- no hand-rolled `useEffect` fetch logic per page.
- **Auth:** JWT stored in `localStorage`, attached by `api/client.ts` to
  every request; `AuthContext` restores the session via `GET /auth/me` on
  load and clears it if the stored token is no longer valid.
- **Styling:** one global stylesheet (`src/index.css`) with CSS custom
  properties and small utility classes (`.card`, `.btn`, `.form-group`,
  `.grid`, ...) -- no component library, no CSS-in-JS. Responsive down to
  phone width via a handful of `@media` rules.
- **File downloads:** authenticated downloads can't use a plain `<a href>`
  (no way to attach a bearer token), so `materialsApi.downloadLatest` /
  `downloadVersion` fetch the file as a blob with the auth header, then
  trigger a save via a programmatic `<a download>` click.

## Pages

Register/login, browse + search public communities, create a
public/private (optionally password-protected) community, join by code,
community detail (subjects, members, join code, leave), create subjects,
search/filter materials (by community, subject, type, text, sort), upload
a material (including question-paper-specific fields), material detail
(version history, download any version, add a new version, favorite,
edit metadata, report), Personal Vault, question-paper filtering, pending
reports (resolve/dismiss), profile.

## Verified

`npm run build` (TypeScript + Vite production build) passes cleanly.
Beyond that, the whole app was manually clicked through against the real
backend + MySQL on 2026-10-20: register, login, logout, protected-route
redirect, create a public and a password-protected private community,
join by code (including wrong-code and wrong-password rejection), add a
subject, upload/search/view/download/favorite/edit a material, confirm
version history is append-only, and confirm empty states for vault,
question papers, and reports. See `../docs/development-guide.md` for the
dated status log.

## Known gaps

- No automated frontend tests yet (Vitest + React Testing Library would
  be the natural choice if added).
- The question-papers filter page takes a raw subject ID rather than a
  friendly subject picker (there's no cross-community subject search
  endpoint on the backend to build one from yet).
