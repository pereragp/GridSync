import { apiRequest } from './client';

export function getReservationHistory(status) {
  const query = status ? `?status=${encodeURIComponent(status)}` : '';
  return apiRequest(`/api/reservations/history${query}`);
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
