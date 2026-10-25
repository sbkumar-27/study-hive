import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Route, BrowserRouter, Routes } from 'react-router-dom'
import { ProtectedRoute } from './shared/components/ProtectedRoute'
import { AuthProvider } from './features/auth/AuthContext'
import { AppLayout } from './shared/layouts/AppLayout'
import { CommunitiesPage } from './features/communities/CommunitiesPage'
import { CommunityDetailPage } from './features/communities/CommunityDetailPage'
import { CreateCommunityPage } from './features/communities/CreateCommunityPage'
import { HomePage } from './features/home/HomePage'
import { LoginPage } from './features/auth/LoginPage'
import { MaterialDetailPage } from './features/materials/MaterialDetailPage'
import { NotFoundPage } from './shared/NotFoundPage'
import { ProfilePage } from './features/profile/ProfilePage'
import { RegisterPage } from './features/auth/RegisterPage'
import { ShareLinkPage } from './features/communities/ShareLinkPage'
import { UploadMaterialPage } from './features/materials/UploadMaterialPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />

            <Route
              element={
                <ProtectedRoute>
                  <AppLayout />
                </ProtectedRoute>
              }
            >
              <Route path="/" element={<HomePage />} />
              <Route path="/communities" element={<CommunitiesPage />} />
              <Route path="/communities/new" element={<CreateCommunityPage />} />
              <Route path="/communities/:id" element={<CommunityDetailPage />} />
              <Route path="/c/:code" element={<ShareLinkPage />} />
              <Route path="/materials/new" element={<UploadMaterialPage />} />
              <Route path="/materials/:id" element={<MaterialDetailPage />} />
              <Route path="/profile" element={<ProfilePage />} />
            </Route>

            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  )
}

export default App
