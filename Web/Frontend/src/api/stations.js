import { apiRequest } from "./client";

export function getStations() {
  return apiRequest("/api/stations");
}

export function getStation(id) {
  return apiRequest(`/api/stations/${id}`);
}

export function createStation(payload) {
  return apiRequest("/api/stations/create", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateStation(id, payload) {
  return apiRequest(`/api/stations/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function updateStationSchedule(id, payload) {
  return apiRequest(`/api/stations/${id}/schedule`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export function deactivateStation(id) {
  return apiRequest(`/api/stations/${id}/deactivate`, {
    method: "POST",
  });
}

export function reactivateStation(id) {
  return apiRequest(`/api/stations/${id}/reactivate`, {
    method: "POST",
  });
}
