// Matches the backend default (studyhive.storage.max-upload-size-mb / MAX_UPLOAD_SIZE_MB).
// There's no config endpoint to read the live value from, so this is the same fixed default the
// backend ships with; if an operator changes MAX_UPLOAD_SIZE_MB, this client-side pre-check may
// drift, but the backend's own validation remains authoritative either way.
export const MAX_UPLOAD_SIZE_MB = 25
export const MAX_UPLOAD_SIZE_BYTES = MAX_UPLOAD_SIZE_MB * 1024 * 1024

/** Clear, immediate feedback before ever attempting the upload -- avoids the ambiguous
 * "Failed to fetch" some setups produce when a request body is rejected for being too large. */
export function fileTooLargeMessage(file: File): string | null {
  if (file.size > MAX_UPLOAD_SIZE_BYTES) {
    return `File too large (max ${MAX_UPLOAD_SIZE_MB} MB)`
  }
  return null
}
