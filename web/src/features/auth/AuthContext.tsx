import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { authApi, type LoginRequest, type RegisterRequest } from './auth'
import { setAuthToken } from '../../shared/api/client'
import type { User } from '../../shared/types'

const STORAGE_KEY = 'studyhive.token'

interface AuthContextValue {
  user: User | null
  token: string | null
  isLoading: boolean
  login: (req: LoginRequest) => Promise<void>
  register: (req: RegisterRequest) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null)
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (!stored) {
      setIsLoading(false)
      return
    }
    setAuthToken(stored)
    authApi
      .me()
      .then((me) => {
        setToken(stored)
        setUser(me)
      })
      .catch(() => {
        localStorage.removeItem(STORAGE_KEY)
        setAuthToken(null)
      })
      .finally(() => setIsLoading(false))
  }, [])

  const applySession = useCallback((newToken: string, newUser: User) => {
    localStorage.setItem(STORAGE_KEY, newToken)
    setAuthToken(newToken)
    setToken(newToken)
    setUser(newUser)
  }, [])

  const login = useCallback(
    async (req: LoginRequest) => {
      const res = await authApi.login(req)
      applySession(res.token, res.user)
    },
    [applySession],
  )

  const register = useCallback(
    async (req: RegisterRequest) => {
      const res = await authApi.register(req)
      applySession(res.token, res.user)
    },
    [applySession],
  )

  const logout = useCallback(() => {
    localStorage.removeItem(STORAGE_KEY)
    setAuthToken(null)
    setToken(null)
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({ user, token, isLoading, login, register, logout }),
    [user, token, isLoading, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
