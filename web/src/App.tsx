import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Navigate, Route, BrowserRouter, Routes } from 'react-router-dom'
import { ProtectedRoute } from './components/ProtectedRoute'
import { AuthProvider } from './context/AuthContext'
import { AppLayout } from './layouts/AppLayout'
import { CommunitiesPage } from './pages/CommunitiesPage'
import { CommunityDetailPage } from './pages/CommunityDetailPage'
import { CreateCommunityPage } from './pages/CreateCommunityPage'
import { LoginPage } from './pages/LoginPage'
import { MaterialDetailPage } from './pages/MaterialDetailPage'
import { MaterialsPage } from './pages/MaterialsPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { ProfilePage } from './pages/ProfilePage'
import { QuestionPapersPage } from './pages/QuestionPapersPage'
import { RegisterPage } from './pages/RegisterPage'
import { ReportsPage } from './pages/ReportsPage'
import { ShareLinkPage } from './pages/ShareLinkPage'
import { UploadMaterialPage } from './pages/UploadMaterialPage'
import { VaultPage } from './pages/VaultPage'

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
              <Route path="/" element={<Navigate to="/communities" replace />} />
              <Route path="/communities" element={<CommunitiesPage />} />
              <Route path="/communities/new" element={<CreateCommunityPage />} />
              <Route path="/communities/:id" element={<CommunityDetailPage />} />
              <Route path="/c/:code" element={<ShareLinkPage />} />
              <Route path="/materials" element={<MaterialsPage />} />
              <Route path="/materials/new" element={<UploadMaterialPage />} />
              <Route path="/materials/:id" element={<MaterialDetailPage />} />
              <Route path="/vault" element={<VaultPage />} />
              <Route path="/question-papers" element={<QuestionPapersPage />} />
              <Route path="/reports" element={<ReportsPage />} />
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
