import { Outlet } from 'react-router-dom'
import { Navbar } from '../components/Navbar'

export function AppLayout() {
  return (
    <>
      <Navbar />
      <main className="page">
        <div className="container">
          <Outlet />
        </div>
      </main>
    </>
  )
}
