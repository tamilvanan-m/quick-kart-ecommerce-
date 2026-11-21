import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authApi } from '../services/customerApi';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const raw = localStorage.getItem('qk_user');
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  });
  const [loading, setLoading] = useState(true);

  const syncUser = useCallback(async () => {
    const token = localStorage.getItem('qk_access_token');
    if (!token) {
      setUser(null);
      setLoading(false);
      return;
    }
    try {
      const profile = await authApi.me();
      setUser(profile);
      localStorage.setItem('qk_user', JSON.stringify(profile));
    } catch {
      localStorage.removeItem('qk_access_token');
      localStorage.removeItem('qk_refresh_token');
      localStorage.removeItem('qk_user');
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    syncUser();
  }, [syncUser]);

  const login = async (email, password) => {
    const res = await authApi.login({ email, password });
    localStorage.setItem('qk_access_token', res.accessToken);
    localStorage.setItem('qk_refresh_token', res.refreshToken);
    localStorage.setItem('qk_user', JSON.stringify(res.user));
    setUser(res.user);
    return res.user;
  };

  const register = async (payload) => {
    const res = await authApi.register(payload);
    localStorage.setItem('qk_access_token', res.accessToken);
    localStorage.setItem('qk_refresh_token', res.refreshToken);
    localStorage.setItem('qk_user', JSON.stringify(res.user));
    setUser(res.user);
    return res.user;
  };

  const logout = () => {
    localStorage.removeItem('qk_access_token');
    localStorage.removeItem('qk_refresh_token');
    localStorage.removeItem('qk_user');
    setUser(null);
  };

  const hasRole = (role) => Boolean(user?.roles?.includes(role));
  const isAdminStaff = Boolean(
    user?.roles?.some((r) => ['OWNER', 'ADMIN', 'STAFF'].includes(r))
  );
  const isOwner = hasRole('OWNER');

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        isAuthenticated: Boolean(user),
        isAdminStaff,
        isOwner,
        hasRole,
        login,
        register,
        logout,
        refreshProfile: syncUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
