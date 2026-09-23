const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:5269";

const SESSION_KEY = "gridsync_session";

export function getSession() {
  try {
    const raw = localStorage.getItem(SESSION_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function saveSession(session) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY);
}

export async function apiRequest(path, options = {}) {
  const session = getSession();
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {}),
  };

  if (session?.token) {
    headers.Authorization = `Bearer ${session.token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    clearSession();
    if (!window.location.pathname.startsWith("/login")) {
      window.location.href = "/login";
    }
    const err = await readError(response);
    throw new Error(err || "Unauthorized");
  }

  if (!response.ok) {
    const err = await readError(response);
    throw new Error(err || `Request failed (${response.status})`);
  }

  if (response.status === 204) {
    return null;
  }

  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

async function readError(response) {
  try {
    const data = await response.json();
    return data.message || data.title || null;
  } catch {
    return null;
  }
}
