import type { ManagedUser, Role, UserStatus } from '../types'
import { api, toApiRole, toManaged, type ApiUser } from './api'

export interface UserInput { name: string; username: string; email: string; role: Role; status: UserStatus }

export const getUsers = async (): Promise<ManagedUser[]> => (await api<ApiUser[]>('GET', '/users')).map(toManaged)
export const getUserById = async (id: string): Promise<ManagedUser> => toManaged(await api<ApiUser>('GET', `/users/${id}`))
export const createUser = async (i: UserInput & { password: string }): Promise<ManagedUser> =>
  toManaged(await api<ApiUser>('POST', '/users', { ...i, role: toApiRole(i.role) }))
export const updateUser = async (id: string, i: UserInput): Promise<ManagedUser> =>
  toManaged(await api<ApiUser>('PUT', `/users/${id}`, { ...i, role: toApiRole(i.role) }))
export const deleteUser = (id: string): Promise<void> => api<void>('DELETE', `/users/${id}`)
export const updateUserStatus = async (id: string, status: UserStatus): Promise<ManagedUser> =>
  toManaged(await api<ApiUser>('PATCH', `/users/${id}/${status === 'ACTIVE' ? 'activate' : 'deactivate'}`))
