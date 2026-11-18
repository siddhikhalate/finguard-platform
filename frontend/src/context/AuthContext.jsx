import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../api/client';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('finguard_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [token, setToken] = useState(() => localStorage.getItem('finguard_token'));
  const [loading, setLoading] = useState(false);

  const login = async (username, password) => {
    setLoading(true);
    try {
      const res = await api.post('/auth/login', { username, password });
      const authData = res.data;
      localStorage.setItem('finguard_token', authData.accessToken);
      localStorage.setItem('finguard_user', JSON.stringify({
        id: authData.userId,
        username: authData.username,
        email: authData.email,
        roles: authData.roles
      }));
      setToken(authData.accessToken);
      setUser({
        id: authData.userId,
        username: authData.username,
        email: authData.email,
        roles: authData.roles
      });
      return { success: true };
    } catch (err) {
      return { success: false, error: err.message || 'Login failed' };
    } finally {
      setLoading(false);
    }
  };

  const register = async (userData) => {
    setLoading(true);
    try {
      const res = await api.post('/auth/register', userData);
      const authData = res.data;
      localStorage.setItem('finguard_token', authData.accessToken);
      localStorage.setItem('finguard_user', JSON.stringify({
        id: authData.userId,
        username: authData.username,
        email: authData.email,
        roles: authData.roles
      }));
      setToken(authData.accessToken);
      setUser({
        id: authData.userId,
        username: authData.username,
        email: authData.email,
        roles: authData.roles
      });
      return { success: true };
    } catch (err) {
      return { success: false, error: err.message || 'Registration failed' };
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem('finguard_token');
    localStorage.removeItem('finguard_user');
    setToken(null);
    setUser(null);
  };

  const hasRole = (role) => {
    if (!user || !user.roles) return false;
    const normalized = role.startsWith('ROLE_') ? role : 'ROLE_' + role;
    return user.roles.includes(normalized);
  };

  return (
    <AuthContext.Provider value={{ user, token, loading, login, register, logout, hasRole }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
