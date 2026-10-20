# StudyHive desktop client

JavaFX 21 (FXML + CSS) client, using Java's built-in `HttpClient` + Jackson
to talk to the same backend REST API as the web client -- it never
touches MySQL directly. See [`../docs/architecture.md`](../docs/architecture.md).

## Run locally (Windows)

1. Start the backend first (see [`../backend/README.md`](../backend/README.md)).
2. From the repo root: `scripts\dev-desktop.bat` (runs `mvn javafx:run`).

The session token is saved to `%USERPROFILE%\.studyhive\session.properties`
so you stay logged in across restarts (matches the web client's
`localStorage` persistence, just on the desktop side).

## Architecture

- **`api/`** -- `ApiClient` is the only class that speaks HTTP: bearer
  token, JSON (de)serialization via Jackson, hand-built
  `multipart/form-data` bodies (the JDK `HttpClient` has no multipart
  support), and raw file downloads. One `Api*Client` wrapper per backend
  resource (`AuthApi`, `CommunitiesApi`, `MaterialsApi`, ...), mirroring
  `web/src/api/*.ts`.
- **`model/`** -- Java records mirroring the backend DTOs 1:1 (same
  fields as `web/src/types/index.ts`).
- **`config/AppContext`** -- a process-wide singleton wiring the API
  client and session together. FXML controllers are instantiated by
  `FXMLLoader` without constructor injection, so this is the simplest
  practical way to share one `HttpClient` and auth state across every
  screen.
- **`navigation/Router`** -- owns the single `Stage`. Login/Register
  replace the whole scene (no nav bar while logged out); once inside the
  app, `navigate()` swaps content inside the persistent `MainShellController`
  shell, mirroring the web client's `AppLayout`/`Outlet` pattern.
  `NavParams` carries arguments between screens (e.g. which community or
  material to show).
- **`util/Async`** -- every API call runs on a background `Task` so the
  JavaFX Application Thread is never blocked; results/errors come back
  via `onSucceeded`/`onFailed`.

## Screens

Mirrors the web client: login/register, browse/search/join communities
(public and password-protected private), create a community, community
detail (subjects, members, join code, leave), create subjects,
search/filter materials, upload a material (native `FileChooser`,
including question-paper fields), material detail (version history,
download any version via `FileChooser` save dialog, add a new version,
favorite, edit metadata, report), Personal Vault, question-paper
filtering, pending reports (resolve/dismiss), profile.

## Verified

`mvn compile` produces all 83 classes cleanly. The app was launched with
`mvn javafx:run` against the real backend + MySQL: it starts with no
exceptions, the native JavaFX graphics libraries load correctly, and the
window opens with the correct title (confirmed via the OS process list --
`MainWindowTitle: StudyHive`).

**Known gap:** this environment has no GUI automation tools for desktop
apps (only for the browser), so unlike the backend and web client, the
screens were *not* clicked through end-to-end the same way. The API
layer reuses the exact same request/response contracts already verified
working from the web client and from curl testing (same endpoints, same
multipart structure, same JWT flow), which gives reasonable confidence,
but actually exercising every screen (search, upload, download, edit,
favorite, report, version history, join flow) by clicking through it on
a real desktop has not been done. Please click through it and report
anything that looks wrong.
