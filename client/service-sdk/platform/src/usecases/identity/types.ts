export type UserRole = 'ROLE_MEMBER' | 'ROLE_ADMIN';

export interface UserResponse {
  id: string;
  email: string;
  role: UserRole;
}

export interface SessionResponse {
  accessToken: string;
  tokenType: 'Bearer';
  /** Seconds. */
  expiresIn: number;
  user: UserResponse;
}
