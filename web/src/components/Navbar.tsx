import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  if (!user) return null

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <nav className="navbar">
      <div className="navbar-inner">
        <NavLink to="/communities" className="navbar-brand">
          StudyHive
        </NavLink>
        <div className="navbar-links">
          <NavLink to="/communities">Communities</NavLink>
          <NavLink to="/vault">My Vault</NavLink>
          <NavLink to="/question-papers">Question Papers</NavLink>
          <NavLink to="/reports">Reports</NavLink>
          <NavLink to="/profile">{user.displayName}</NavLink>
          <button className="btn btn-sm" onClick={handleLogout}>
            Log out
          </button>
        </div>
      </div>
    </nav>
  )
}
