/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Backend origin only, e.g. https://studyhive-backend.onrender.com -- no trailing slash, no /api/v1. */
  readonly VITE_API_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
