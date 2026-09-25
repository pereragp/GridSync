import { apiRequest } from './client';

export function getReservationHistory(status) {
  const query = status ? `?status=${encodeURIComponent(status)}` : '';
  return apiRequest(`/api/reservations/history${query}`);
}

export function getManagedReservations(status) {
  const query = status ? `?status=${encodeURIComponent(status)}` : '';
  return apiRequest(`/api/reservations/manage${query}`);
}

export function createReservation(payload) {
  return apiRequest('/api/reservations', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function updateReservation(id, payload) {
  return apiRequest(`/api/reservations/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export function cancelReservation(id, reason) {
  return apiRequest(`/api/reservations/${id}/cancel`, {
    method: 'POST',
    body: JSON.stringify({ reason: reason || null }),
  });
}

export function approveReservation(id) {
  return apiRequest(`/api/reservations/${id}/approve`, {
    method: 'POST',
  });
}

export function rejectReservation(id, reason) {
  return apiRequest(`/api/reservations/${id}/reject`, {
    method: 'POST',
    body: JSON.stringify({ reason }),
  });
}
