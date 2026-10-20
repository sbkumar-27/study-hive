import { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { Icon } from './Icon'

const LINKS = [
  { to: '/communities', label: 'Communities', icon: 'communities' as const },
  { to: '/materials', label: 'Materials', icon: 'materials' as const },
  { to: '/vault', label: 'My Vault', icon: 'vault' as const },
  { to: '/question-papers', label: 'Question Papers', icon: 'papers' as const },
  { to: '/reports', label: 'Reports', icon: 'reports' as const },
]

function initialsOf(name: string): string {
  const parts = name.trim().split(/\s+/)
  const first = parts[0]?.[0] ?? ''
  const last = parts.length > 1 ? parts[parts.length - 1]?.[0] ?? '' : ''
  return (first + last).toUpperCase() || '?'
}

export function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)

  if (!user) return null

  function handleLogout() {
    logout()
    navigate('/login')
  }

  function closeMenu() {
    setMenuOpen(false)
  }

  return (
    <nav className="navbar">
      <div className="navbar-inner">
        <NavLink to="/communities" className="navbar-brand" onClick={closeMenu}>
          <span className="navbar-logo">SH</span>
          StudyHive
        </NavLink>

        <button
          type="button"
          className="navbar-toggle"
          aria-label={menuOpen ? 'Close navigation menu' : 'Open navigation menu'}
          aria-expanded={menuOpen}
          onClick={() => setMenuOpen((v) => !v)}
        >
          <Icon name={menuOpen ? 'close' : 'menu'} />
        </button>

        <div className={`navbar-links ${menuOpen ? 'open' : ''}`}>
          {LINKS.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}
              onClick={closeMenu}
            >
              <Icon name={link.icon} size={16} />
              {link.label}
            </NavLink>
          ))}
          <NavLink to="/profile" className="nav-link" onClick={closeMenu}>
            <span className="avatar" aria-hidden="true">
              {initialsOf(user.displayName)}
            </span>
            {user.displayName}
          </NavLink>
          <button className="btn btn-sm" onClick={handleLogout}>
            <Icon name="logout" size={15} />
            Log out
          </button>
        </div>
      </div>
    </nav>
  )
}
