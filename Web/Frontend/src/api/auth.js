import { apiRequest } from "./client";

export function login(email, password) {
  return apiRequest("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
}

export function logout() {
  return apiRequest("/api/auth/logout", { method: "POST" });
}

export function forgotPassword(email) {
  return apiRequest("/api/auth/forgot-password", {
    method: "POST",
    body: JSON.stringify({ email }),
  });
}

export function resetPassword(email, resetToken, newPassword) {
  return apiRequest("/api/auth/reset-password", {
    method: "POST",
    body: JSON.stringify({ email, resetToken, newPassword }),
  });
}

export function changePassword(currentPassword, newPassword) {
  return apiRequest("/api/auth/change-password", {
    method: "POST",
    body: JSON.stringify({ currentPassword, newPassword }),
  });
}
