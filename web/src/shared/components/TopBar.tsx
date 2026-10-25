import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../features/auth/AuthContext'
import { searchApi } from '../../features/search/search'
import { Icon } from './Icon'
import type { SearchResult } from '../types'

function initialsOf(name: string): string {
  const parts = name.trim().split(/\s+/)
  const first = parts[0]?.[0] ?? ''
  const last = parts.length > 1 ? parts[parts.length - 1]?.[0] ?? '' : ''
  return (first + last).toUpperCase() || '?'
}

export function TopBar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)
  const [q, setQ] = useState('')
  const [results, setResults] = useState<SearchResult[]>([])
  const [searching, setSearching] = useState(false)
  const [showResults, setShowResults] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)
  const searchRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    function onClickOutside(e: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) setMenuOpen(false)
      if (searchRef.current && !searchRef.current.contains(e.target as Node)) setShowResults(false)
    }
    document.addEventListener('mousedown', onClickOutside)
    return () => document.removeEventListener('mousedown', onClickOutside)
  }, [])

  useEffect(() => {
    const term = q.trim()
    if (!term) {
      setResults([])
      return
    }
    setSearching(true)
    const handle = window.setTimeout(() => {
      searchApi
        .search(term, 0, 6)
        .then((res) => {
          setResults(res.content)
          setShowResults(true)
        })
        .catch(() => setResults([]))
        .finally(() => setSearching(false))
    }, 300)
    return () => window.clearTimeout(handle)
  }, [q])

  if (!user) return null

  function handleLogout() {
    setMenuOpen(false)
    logout()
    navigate('/login')
  }

  return (
    <nav className="topbar">
      <div className="topbar-inner">
        <Link to="/" className="topbar-brand">
          <span className="topbar-logo">SH</span>
          StudyHive
        </Link>

        <div className="topbar-search" ref={searchRef}>
          <span className="topbar-search-icon">
            <Icon name="search" size={16} />
          </span>
          <input
            placeholder="Search materials and question papers..."
            value={q}
            onChange={(e) => setQ(e.target.value)}
            onFocus={() => results.length > 0 && setShowResults(true)}
          />
          {showResults && (
            <div className="topbar-search-results">
              {searching && (
                <div className="topbar-search-result">
                  <span className="text-sm muted">Searching...</span>
                </div>
              )}
              {!searching && results.length === 0 && (
                <div className="topbar-search-result">
                  <span className="text-sm muted">No results</span>
                </div>
              )}
              {!searching &&
                results.map((r) => (
                  <Link
                    key={`${r.kind}-${r.material.materialId}`}
                    to={`/materials/${r.material.materialId}`}
                    className="topbar-search-result"
                    onClick={() => {
                      setShowResults(false)
                      setQ('')
                    }}
                  >
                    <strong className="text-sm">{r.material.title}</strong>
                    <span className="text-sm muted">
                      {r.material.materialType.replace('_', ' ')}
                      {r.material.subjectCode ? ` -- ${r.material.subjectCode}` : ''}
                    </span>
                  </Link>
                ))}
            </div>
          )}
        </div>

        <div className="topbar-actions">
          <Link to="/materials/new" className="btn btn-primary btn-sm">
            <Icon name="upload" size={15} />
            Upload
          </Link>

          <div className="profile-menu" ref={menuRef}>
            <button
              type="button"
              className="profile-menu-trigger"
              onClick={() => setMenuOpen((v) => !v)}
              aria-haspopup="menu"
              aria-expanded={menuOpen}
            >
              <span className="avatar" aria-hidden="true">
                {initialsOf(user.displayName)}
              </span>
              <span className="text-sm profile-name">{user.displayName}</span>
            </button>
            {menuOpen && (
              <div className="profile-menu-dropdown" role="menu">
                <Link to="/profile" className="profile-menu-item" role="menuitem" onClick={() => setMenuOpen(false)}>
                  <Icon name="user" size={15} />
                  Profile
                </Link>
                <Link
                  to="/profile?tab=vault"
                  className="profile-menu-item"
                  role="menuitem"
                  onClick={() => setMenuOpen(false)}
                >
                  <Icon name="vault" size={15} />
                  My Vault
                </Link>
                <button type="button" className="profile-menu-item" role="menuitem" onClick={handleLogout}>
                  <Icon name="logout" size={15} />
                  Log out
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </nav>
  )
}
