import { apiRequest } from "./client";

export function createStaff(payload) {
  return apiRequest("/api/users/staff", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function registerProsumer(payload) {
  return apiRequest("/api/users/prosumers/register", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function getUsers() {
  return apiRequest("/api/users");
}

export function getPendingUsers() {
  return apiRequest("/api/users/pending");
}

export function getUser(id) {
  return apiRequest(`/api/users/${id}`);
}

export function updateUser(id, payload) {
  return apiRequest(`/api/users/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function approveUser(id) {
  return apiRequest(`/api/users/${id}/approve`, { method: "POST" });
}

export function requestDeactivation(id) {
  return apiRequest(`/api/users/${id}/request-deactivation`, { method: "POST" });
}

export function deactivateUser(id) {
  return apiRequest(`/api/users/${id}/deactivate`, { method: "POST" });
}

export function reactivateUser(id, backofficeUserId) {
  return apiRequest(
    `/api/users/${id}/reactivate?backofficeUserId=${encodeURIComponent(backofficeUserId)}`,
    { method: "POST" }
  );
}
