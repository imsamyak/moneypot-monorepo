import { useCase } from '@moneypot/sdk-core';
import { credentialsBody, forgotPasswordBody, resetPasswordBody } from './schemas.js';
import type { SessionResponse, UserResponse } from './types.js';

export type * from './types.js';

/** Anyone can sign up; the account is a member. */
export const register = useCase('Register')
  .service('identity')
  .post('/api/auth/register')
  .body(credentialsBody)
  .storesToken()
  .returns<SessionResponse>()
  .build();

export const login = useCase('Login')
  .service('identity')
  .post('/api/auth/login')
  .body(credentialsBody)
  .storesToken()
  .returns<SessionResponse>()
  .build();

/** Tokens are stateless, so this only clears the stored one. It sends none, so an expired session can still log out. */
export const logout = useCase('Logout')
  .service('identity')
  .post('/api/auth/logout')
  .clearsToken()
  .build();

export const getCurrentUser = useCase('GetCurrentUser')
  .service('identity')
  .get('/api/auth/me')
  .authRequired()
  .returns<UserResponse>()
  .build();

/** Always 202, whether or not the address has an account. */
export const forgotPassword = useCase('ForgotPassword')
  .service('identity')
  .post('/api/auth/forgot-password')
  .body(forgotPasswordBody)
  .build();

export const resetPassword = useCase('ResetPassword')
  .service('identity')
  .post('/api/auth/reset-password')
  .body(resetPasswordBody)
  .build();

export const identityUseCases = [register, login, logout, getCurrentUser, forgotPassword, resetPassword] as const;
