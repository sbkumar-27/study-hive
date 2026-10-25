import { api } from '../../shared/api/client'
import type { AuthResponse, User } from '../../shared/types'

export interface RegisterRequest {
  username: string
  email: string
  password: string
  displayName: string
}

export interface LoginRequest {
  usernameOrEmail: string
  password: string
}

export const authApi = {
  register: (req: RegisterRequest) => api.post<AuthResponse>('/auth/register', req),
  login: (req: LoginRequest) => api.post<AuthResponse>('/auth/login', req),
  me: () => api.get<User>('/auth/me'),
}
