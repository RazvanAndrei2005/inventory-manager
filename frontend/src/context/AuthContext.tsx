"use client";

import React, { createContext, useCallback, useContext, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  apiFetch,
  clearStoredTokens,
  getStoredTokens,
  setStoredTokens,
} from "@/lib/api";
import type { JwtResponse, UserResponse } from "@/types/api";

interface AuthContextValue {
  user: UserResponse | null;
  loading: boolean;
  isAdmin: boolean;
  isManagerOrAdmin: boolean;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const router = useRouter();

  const refreshProfile = useCallback(async () => {
    const { accessToken, refreshToken } = getStoredTokens();
    if (!accessToken && !refreshToken) {
      setUser(null);
      setLoading(false);
      return;
    }
    try {
      const me = await apiFetch<UserResponse>("/api/users/me");
      setUser(me);
    } catch {
      clearStoredTokens();
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refreshProfile();
  }, [refreshProfile]);

  const login = async (username: string, password: string) => {
    const data = await apiFetch<JwtResponse>("/api/auth/login", {
      method: "POST",
      skipAuth: true,
      body: JSON.stringify({ username, password }),
    });
    setStoredTokens(data.accessToken, data.refreshToken);
    const me = await apiFetch<UserResponse>("/api/users/me");
    setUser(me);
    router.push("/");
  };

  const register = async (username: string, email: string, password: string) => {
    await apiFetch<string>("/api/auth/register", {
      method: "POST",
      skipAuth: true,
      body: JSON.stringify({ username, email, password }),
    });
  };

  const logout = async () => {
    const { refreshToken } = getStoredTokens();
    if (refreshToken) {
      try {
        await apiFetch<void>("/api/auth/logout", {
          method: "POST",
          skipAuth: true,
          body: JSON.stringify({ refreshToken }),
        });
      } catch {
        // ignore logout error if token already expired/revoked
      }
    }
    clearStoredTokens();
    setUser(null);
    router.push("/login");
  };

  const isAdmin = user?.role === "ROLE_ADMIN";
  const isManagerOrAdmin = user?.role === "ROLE_ADMIN" || user?.role === "ROLE_MANAGER";

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        isAdmin,
        isManagerOrAdmin,
        login,
        register,
        logout,
        refreshProfile,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return ctx;
}
