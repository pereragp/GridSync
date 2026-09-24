import { createContext, useContext, useMemo, useState } from "react";
import * as authApi from "../api/auth";
import { clearSession, getSession, saveSession } from "../api/client";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [session, setSession] = useState(() => getSession());

  const value = useMemo(() => {
    async function login(email, password) {
      const data = await authApi.login(email, password);
      const next = {
        token: data.token,
        userId: data.userId,
        fullName: data.fullName,
        email: data.email,
        role: data.role,
        status: data.status,
        nic: data.nic ?? null,
        expiresAt: data.expiresAt,
      };
      saveSession(next);
      setSession(next);
      return next;
    }

    async function logout() {
      try {
        if (getSession()?.token) {
          await authApi.logout();
        }
      } catch {
        // Still clear local session even if API logout fails.
      }
      clearSession();
      setSession(null);
    }

    function homePathFor(role) {
      if (role === "Backoffice") return "/backoffice";
      if (role === "GridOperator") return "/operator";
      if (role === "Prosumer") return "/prosumer";
      return "/profile";
    }

    return {
      session,
      user: session,
      isAuthenticated: Boolean(session?.token),
      login,
      logout,
      homePathFor,
    };
  }, [session]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return ctx;
}
