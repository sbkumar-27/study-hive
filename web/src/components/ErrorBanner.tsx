import { ApiError } from '../api/client'
import { Icon } from './Icon'

export function errorMessage(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.fieldErrors?.length) return err.fieldErrors.join('; ')
    return err.message
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
