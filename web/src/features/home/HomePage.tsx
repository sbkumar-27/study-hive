import { CommunitySidebarList } from '../../shared/components/CommunitySidebarList'
import { ActivityFeed } from '../../shared/components/ActivityFeed'

export function HomePage() {
  return (
    <div className="home-layout">
      <CommunitySidebarList />
      <ActivityFeed />
    </div>
  )
}
