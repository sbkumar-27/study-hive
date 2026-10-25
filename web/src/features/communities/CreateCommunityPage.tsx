import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { communitiesApi } from './communities'
import { ErrorBanner } from '../../shared/components/ErrorBanner'
import { Icon } from '../../shared/components/Icon'
import type { CommunityType } from '../../shared/types'

export function CreateCommunityPage() {
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [communityType, setCommunityType] = useState<CommunityType>('PUBLIC')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<unknown>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const community = await communitiesApi.create({
        name,
        description: description || undefined,
        communityType,
        privatePassword: communityType === 'PRIVATE' && password ? password : undefined,
      })
      navigate(`/communities/${community.communityId}`)
    } catch (err) {
      setError(err)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="stack">
      <h1>Create a community</h1>
      <form className="form form-wide card" onSubmit={handleSubmit}>
        <ErrorBanner error={error} />
        <div className="form-group">
          <label htmlFor="name">Name</label>
          <input id="name" value={name} onChange={(e) => setName(e.target.value)} required autoFocus />
        </div>
        <div className="form-group">
          <label htmlFor="description">Description (optional)</label>
          <textarea id="description" value={description} onChange={(e) => setDescription(e.target.value)} />
        </div>
        <div className="form-group">
          <label htmlFor="communityType">Visibility</label>
          <select
            id="communityType"
            value={communityType}
            onChange={(e) => setCommunityType(e.target.value as CommunityType)}
          >
            <option value="PUBLIC">Public -- discoverable by anyone, joinable with the code</option>
            <option value="PRIVATE">Private -- not listed; join code required (optionally a password too)</option>
          </select>
        </div>
        {communityType === 'PRIVATE' && (
          <div className="form-group">
            <label htmlFor="password">Password (optional, extra layer beyond the join code)</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              minLength={4}
            />
          </div>
        )}
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? (
            'Creating...'
          ) : (
            <>
              <Icon name="plus" size={16} />
              Create community
            </>
          )}
        </button>
      </form>
    </div>
  )
}
