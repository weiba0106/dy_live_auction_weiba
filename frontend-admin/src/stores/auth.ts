import { create } from 'zustand'

interface AuthState {
  token: string | null
  userId: number | null
  nickname: string | null
  role: number | null
  setAuth: (token: string, userId: number, nickname: string, role: number) => void
  logout: () => void
  isLoggedIn: () => boolean
}

export const useAuthStore = create<AuthState>((set, get) => ({
  token: localStorage.getItem('token'),
  userId: Number(localStorage.getItem('userId')) || null,
  nickname: localStorage.getItem('nickname'),
  role: Number(localStorage.getItem('role')) || null,

  setAuth: (token, userId, nickname, role) => {
    localStorage.setItem('token', token)
    localStorage.setItem('userId', String(userId))
    localStorage.setItem('nickname', nickname)
    localStorage.setItem('role', String(role))
    set({ token, userId, nickname, role })
  },

  logout: () => {
    localStorage.clear()
    set({ token: null, userId: null, nickname: null, role: null })
  },

  isLoggedIn: () => !!get().token,
}))
