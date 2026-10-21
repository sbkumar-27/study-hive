import { ApiError } from '../api/client'
import { Icon } from './Icon'

export function errorMessage(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.fieldErrors?.length) return err.fieldErrors.join('; ')
    return err.message
  }
  // A raw browser TypeError ("Failed to fetch" / "NetworkError when attempting to fetch
  // resource...") means the request never got an HTTP response at all -- the server refused
  // or reset the connection before replying. For uploads this is most often an oversized file
  // (some setups reset the connection outright instead of returning a clean 413/400), but it can
  // also be a genuine connectivity problem, so this doesn't claim to know which for certain.
  if (err instanceof TypeError) {
    return 'Could not complete the request. If you were uploading a file, it may be too large ' +
      '(max 25 MB) -- otherwise, check your connection and try again.'
  }
  if (err instanceof Error) return err.message
  return 'Something went wrong. Please try again.'
}

export function ErrorBanner({ error }: { error: unknown }) {
  if (!error) return null
  return (
    <div className="alert alert-error" role="alert">
      <Icon name="warning" size={16} />
      <span>{errorMessage(error)}</span>
    </div>
  )
}
