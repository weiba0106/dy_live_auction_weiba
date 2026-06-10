import api from './index'
import type { LoginRequest, RegisterRequest, LoginResponse } from '../types'

export const authApi = {
  login: (data: LoginRequest) => api.post<any, LoginResponse>('/auth/login', data),
  register: (data: RegisterRequest) => api.post<any, LoginResponse>('/auth/register', data),
}
