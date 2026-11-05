"use client";
import { createContext, useContext, useEffect, useState, useCallback } from "react";
import * as api from "@/lib/api";
import { getSession, subscribe } from "@/lib/tokenStore";

interface AuthContextValue {
  user: api.AuthUser | null;
  loading: boolean;
  login: (input: api.LoginInput) => Promise<void>;
  register: (input: api.RegisterInput) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<api.AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Keep local state in sync if the session changes in another tab/component.
    const unsubscribe = subscribe((session) => setUser(session ? { name: session.name, role: session.role as api.Role } : null));

    const existing = getSession();
    if (existing) setUser({ name: existing.name, role: existing.role as api.Role });

    // Validate/refresh on load rather than trusting a possibly-expired stored access token.
    api.restoreSession().then((restored) => {
      setUser(restored);
      setLoading(false);
    });

    return unsubscribe;
  }, []);

  const login = useCallback(async (input: api.LoginInput) => {
    setUser(await api.login(input));
  }, []);

  const register = useCallback(async (input: api.RegisterInput) => {
    setUser(await api.register(input));
  }, []);

  const logout = useCallback(async () => {
    await api.logout();
    setUser(null);
  }, []);

  return <AuthContext.Provider value={{ user, loading, login, register, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
