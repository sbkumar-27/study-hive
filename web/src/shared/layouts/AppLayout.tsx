import { Outlet } from 'react-router-dom'
import { TopBar } from '../components/TopBar'

export function AppLayout() {
  return (
    <>
      <TopBar />
      <main className="page">
        <div className="container">
          <Outlet />
        </div>
      </main>
    </>
  )
}
