"use client";

/**
 * Tokens live in memory + localStorage, not an httpOnly cookie — the simplest thing that
 * works for a prototype with no backend-for-frontend layer. That does mean they're
 * readable by any script on the page (XSS risk); a production deployment should move to
 * httpOnly, SameSite cookies set by a server-side route instead. Flagged in frontend/README.md.
 */
export interface StoredSession {
  accessToken: string;
  refreshToken: string;
  name: string;
  role: string;
}

const STORAGE_KEY = "reliefgrid.session";
let current: StoredSession | null = null;
const listeners = new Set<(s: StoredSession | null) => void>();

function load(): StoredSession | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as StoredSession) : null;
  } catch {
    return null;
  }
}

export function getSession(): StoredSession | null {
  if (current === null) current = load();
  return current;
}

export function setSession(session: StoredSession | null): void {
  current = session;
  if (typeof window !== "undefined") {
    if (session) window.localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    else window.localStorage.removeItem(STORAGE_KEY);
  }
  listeners.forEach((fn) => fn(session));
}

export function subscribe(fn: (s: StoredSession | null) => void): () => void {
  listeners.add(fn);
  return () => listeners.delete(fn);
}
