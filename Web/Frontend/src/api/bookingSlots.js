import { apiRequest } from "./client";

export function getBookingSlots(stationId, status) {
  const params = new URLSearchParams();
  if (stationId) params.set("stationId", stationId);
  if (status) params.set("status", status);
  const query = params.toString();
  return apiRequest(`/api/bookingslots${query ? `?${query}` : ""}`);
}

export function getBookingSlot(id) {
  return apiRequest(`/api/bookingslots/${id}`);
}

export function updateBookingSlot(id, payload) {
  return apiRequest(`/api/bookingslots/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function closeBookingSlot(id) {
  return apiRequest(`/api/bookingslots/${id}/close`, {
    method: "POST",
  });
}

export function reopenBookingSlot(id) {
  return apiRequest(`/api/bookingslots/${id}/reopen`, {
    method: "POST",
  });
}
