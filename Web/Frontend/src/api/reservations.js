import { apiRequest } from './client';

export function getReservationHistory(status) {
  const query = status ? `?status=${encodeURIComponent(status)}` : '';
  return apiRequest(`/api/reservations/history${query}`);
}

export function getUpcomingReservations(status) {
  const query = status ? `?status=${encodeURIComponent(status)}` : '';
  return apiRequest(`/api/reservations/upcoming${query}`);
}

export function getManagedReservations(status) {
  const query = status ? `?status=${encodeURIComponent(status)}` : '';
  return apiRequest(`/api/reservations/manage${query}`);
}

export function getAvailableBookingSlots() {
  return apiRequest('/api/reservations/slots');
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

export function verifyReservationQr(qrPayload) {
  return apiRequest('/api/reservations/verify-qr', {
    method: 'POST',
    body: JSON.stringify({ qrPayload }),
  });
}

export function completeReservation(id) {
  return apiRequest(`/api/reservations/${id}/complete`, {
    method: 'POST',
  });
}

export function getReservationDashboardStats() {
  return apiRequest('/api/reservations/dashboard-stats');
}

export function getProsumerDashboardStats() {
  return apiRequest('/api/reservations/prosumer-dashboard');
}
