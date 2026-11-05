import { getSession, setSession } from "./tokenStore";

export type NeedType = "FOOD" | "WATER" | "MEDICINE" | "SHELTER" | "RESCUE";
export type Urgency = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type Status = "PENDING" | "VERIFIED" | "ASSIGNED" | "FULFILLED";
export type Role = "CITIZEN" | "VOLUNTEER" | "NGO" | "COORDINATOR" | "ADMIN";
/** Roles a person can pick at sign-up — COORDINATOR/ADMIN are provisioned separately, never self-service (see backend AuthService). */
export const SELF_REGISTER_ROLES: Role[] = ["CITIZEN", "VOLUNTEER", "NGO"];

export interface ReliefRequest {
  id: string; needType: NeedType; quantity: number; urgency: Urgency; status: Status;
  lat: number; lng: number; place: string; description: string; createdAt: string;
}
export type NewRequest = Pick<ReliefRequest, "needType" | "quantity" | "description" | "lat" | "lng"> & { lifeAtRisk: boolean };

// Colors only — display labels are translated (see messages/*.json "needs"/"urgency"
// and useTranslations("needs" | "urgency") in components), since this file has no
// knowledge of the active locale.
export const URGENCY_COLOR: Record<Urgency, string> = {
  CRITICAL: "#D93A2B", HIGH: "#EE8A22", MEDIUM: "#D9AA1E", LOW: "#4C9F70",
};
export const NEED_TYPES: NeedType[] = ["FOOD", "WATER", "MEDICINE", "SHELTER", "RESCUE"];
export const URGENCY_LEVELS: Urgency[] = ["CRITICAL", "HIGH", "MEDIUM", "LOW"];

const ago = (m: number) => new Date(Date.now() - m * 60000).toISOString();
export const SEED: ReliefRequest[] = [
  { id: "r1", needType: "RESCUE", quantity: 6, urgency: "CRITICAL", status: "VERIFIED", lat: 6.9553, lng: 79.922, place: "Kelaniya, riverside lane", description: "Six people on a rooftop, water still rising.", createdAt: ago(9) },
  { id: "r2", needType: "MEDICINE", quantity: 40, urgency: "HIGH", status: "PENDING", lat: 6.9896, lng: 79.8918, place: "Wattala, temple shelter", description: "Insulin and blood pressure tablets for elderly residents.", createdAt: ago(35) },
  { id: "r3", needType: "FOOD", quantity: 120, urgency: "HIGH", status: "ASSIGNED", lat: 6.9333, lng: 79.8845, place: "Kolonnawa, school hall", description: "Dry rations for 30 families, no cooking facilities.", createdAt: ago(80) },
  { id: "r4", needType: "WATER", quantity: 300, urgency: "MEDIUM", status: "PENDING", lat: 6.95, lng: 79.993, place: "Biyagama", description: "Wells contaminated. Drinking water needed.", createdAt: ago(130) },
  { id: "r5", needType: "SHELTER", quantity: 25, urgency: "MEDIUM", status: "VERIFIED", lat: 6.9667, lng: 79.8833, place: "Peliyagoda", description: "Tarpaulins and mats for families sleeping outdoors.", createdAt: ago(190) },
  { id: "r6", needType: "FOOD", quantity: 60, urgency: "LOW", status: "FULFILLED", lat: 6.978, lng: 79.928, place: "Kiribathgoda", description: "Baby food and milk powder.", createdAt: ago(400) },
  { id: "r7", needType: "MEDICINE", quantity: 15, urgency: "CRITICAL", status: "PENDING", lat: 6.995, lng: 79.895, place: "Mabola", description: "Dialysis patient cut off by flooded road.", createdAt: ago(14) },
];

const API = process.env.NEXT_PUBLIC_API_URL;
let store = [...SEED];
const wait = (ms = 250) => new Promise((r) => setTimeout(r, ms));

// ---------- Auth ----------
export interface AuthUser {
  name: string;
  role: Role;
}
export interface LoginInput {
  email: string;
  password: string;
}
export interface RegisterInput extends LoginInput {
  name: string;
  role: Role;
}
interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  name: string;
  role: Role;
}

class ApiAuthError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "ApiAuthError";
  }
}

async function parseErrorMessage(res: Response): Promise<string> {
  try {
    const body = await res.json();
    return body?.detail ?? body?.message ?? `Request failed (${res.status})`;
  } catch {
    return `Request failed (${res.status})`;
  }
}

function requireApi(): string {
  if (!API) throw new Error("Auth requires NEXT_PUBLIC_API_URL to be set — there is no demo-mode login.");
  return API;
}

export async function login(input: LoginInput): Promise<AuthUser> {
  const res = await fetch(`${requireApi()}/auth/login`, {
    method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(input),
  });
  if (!res.ok) throw new ApiAuthError(await parseErrorMessage(res));
  const t: TokenResponse = await res.json();
  setSession({ accessToken: t.accessToken, refreshToken: t.refreshToken, name: t.name, role: t.role });
  return { name: t.name, role: t.role };
}

export async function register(input: RegisterInput): Promise<AuthUser> {
  const res = await fetch(`${requireApi()}/auth/register`, {
    method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(input),
  });
  if (!res.ok) throw new ApiAuthError(await parseErrorMessage(res));
  const t: TokenResponse = await res.json();
  setSession({ accessToken: t.accessToken, refreshToken: t.refreshToken, name: t.name, role: t.role });
  return { name: t.name, role: t.role };
}

/** Tries to restore a session from a stored refresh token (e.g. on app load). Returns
 * null, rather than throwing, on any failure — an expired/invalid session is routine,
 * not an error worth surfacing. */
export async function restoreSession(): Promise<AuthUser | null> {
  const session = getSession();
  if (!session || !API) return null;
  try {
    const res = await fetch(`${API}/auth/refresh`, {
      method: "POST", headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken: session.refreshToken }),
    });
    if (!res.ok) { setSession(null); return null; }
    const t: TokenResponse = await res.json();
    setSession({ accessToken: t.accessToken, refreshToken: t.refreshToken, name: t.name, role: t.role });
    return { name: t.name, role: t.role };
  } catch {
    return null;
  }
}

export async function logout(): Promise<void> {
  const session = getSession();
  if (session && API) {
    try {
      await fetch(`${API}/auth/logout`, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken: session.refreshToken }),
      });
    } catch {
      // Same philosophy as elsewhere in this project: a failed logout call server-side
      // never blocks clearing the local session — the user always gets logged out locally.
    }
  }
  setSession(null);
}

/** Attaches the access token, and on a single 401 tries one silent refresh-and-retry
 * before giving up — covers the common case of an access token expiring mid-session. */
async function authFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const session = getSession();
  const withAuth = (token?: string): RequestInit => ({
    ...init,
    headers: { ...(init.headers ?? {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
  });

  let res = await fetch(`${API}${path}`, withAuth(session?.accessToken));
  if (res.status === 401 && session) {
    const refreshed = await restoreSession();
    if (refreshed) res = await fetch(`${API}${path}`, withAuth(getSession()?.accessToken));
  }
  return res;
}

export async function listRequests(): Promise<ReliefRequest[]> {
  if (API) {
    const res = await authFetch("/requests");
    if (!res.ok) throw new Error(await parseErrorMessage(res));
    const page = await res.json();
    return page.content ?? page; // backend wraps list endpoints in a PageResponse
  }
  await wait();
  return [...store];
}
export async function createRequest(n: NewRequest): Promise<ReliefRequest> {
  if (API) {
    const res = await authFetch("/requests", {
      method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(n),
    });
    if (!res.ok) throw new Error(await parseErrorMessage(res));
    return res.json();
  }
  await wait();
  const r: ReliefRequest = {
    id: crypto.randomUUID(), needType: n.needType, quantity: n.quantity, description: n.description,
    lat: n.lat, lng: n.lng, urgency: n.lifeAtRisk ? "CRITICAL" : "MEDIUM", status: "PENDING",
    place: `${n.lat.toFixed(3)}, ${n.lng.toFixed(3)}`, createdAt: new Date().toISOString(),
  };
  store = [r, ...store];
  return r;
}
export async function updateStatus(id: string, status: Status): Promise<void> {
  if (API) {
    const res = await authFetch(`/requests/${id}/status`, {
      method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ status }),
    });
    if (!res.ok) throw new Error(await parseErrorMessage(res));
    return;
  }
  await wait(120);
  store = store.map((r) => (r.id === id ? { ...r, status } : r));
}
export interface PublicStatus {
  openRequests: number;
  fulfilledRequests: number;
  activeVolunteerTasks: number;
  openByUrgency: Partial<Record<Urgency, number>>;
  openByNeedType: Partial<Record<NeedType, number>>;
  generatedAt: string;
}

function demoPublicStatus(): PublicStatus {
  const open = SEED.filter((r) => r.status !== "FULFILLED");
  const count = <T extends string>(items: T[]) =>
    items.reduce<Partial<Record<T, number>>>((acc, v) => ({ ...acc, [v]: (acc[v] ?? 0) + 1 }), {});
  return {
    openRequests: open.length,
    fulfilledRequests: SEED.length - open.length,
    activeVolunteerTasks: open.filter((r) => r.status === "ASSIGNED").length,
    openByUrgency: count(open.map((r) => r.urgency)),
    openByNeedType: count(open.map((r) => r.needType)),
    generatedAt: new Date().toISOString(),
  };
}

export async function getPublicStatus(): Promise<PublicStatus> {
  if (API) return (await fetch(`${API}/public/status`, { cache: "no-store" })).json();
  await wait(150);
  return demoPublicStatus();
}

export interface DailyCount {
  day: string; // YYYY-MM-DD
  needType: NeedType;
  count: number;
}
export interface ForecastPoint {
  day: string;
  projectedCount: number;
}
export interface NeedTypeForecast {
  needType: NeedType;
  history: DailyCount[];
  forecast: ForecastPoint[];
  trend: "rising" | "falling" | "flat";
}

const AI_SERVICE_URL = process.env.NEXT_PUBLIC_AI_SERVICE_URL;

function demoDailyCounts(days: number): DailyCount[] {
  // Synthetic but shaped like real activity: FOOD and WATER trend up (a slow-building
  // crisis), MEDICINE trends down (being resolved), so the chart has something to show.
  const trendFor: Partial<Record<NeedType, (day: number) => number>> = {
    FOOD: (d) => Math.max(0, Math.round(2 + d * 0.6 + Math.sin(d) * 1.5)),
    WATER: (d) => Math.max(0, Math.round(1 + d * 0.4 + Math.cos(d) * 1)),
    MEDICINE: (d) => Math.max(0, Math.round(8 - d * 0.3 + Math.sin(d) * 1)),
  };
  const out: DailyCount[] = [];
  const today = new Date();
  for (let i = days - 1; i >= 0; i--) {
    const date = new Date(today);
    date.setDate(date.getDate() - i);
    const dayStr = date.toISOString().slice(0, 10);
    const dayIndex = days - 1 - i;
    for (const [needType, fn] of Object.entries(trendFor) as [NeedType, (d: number) => number][]) {
      out.push({ day: dayStr, needType, count: fn(dayIndex) });
    }
  }
  return out;
}

export async function getDailyCounts(days = 30): Promise<DailyCount[]> {
  if (API) {
    const res = await authFetch(`/requests/daily-counts?days=${days}`);
    if (!res.ok) throw new Error(await parseErrorMessage(res));
    return res.json();
  }
  await wait(150);
  return demoDailyCounts(days);
}

export async function getForecast(dailyCounts: DailyCount[], horizonDays = 7): Promise<NeedTypeForecast[]> {
  if (AI_SERVICE_URL) {
    return (
      await fetch(`${AI_SERVICE_URL}/forecast`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ dailyCounts, horizonDays }),
      })
    ).json();
  }
  // Demo fallback: a simple client-side linear fit per need type, mirroring the AI
  // service's own logic closely enough to demonstrate the chart without a live service.
  await wait(200);
  const byNeed = new Map<NeedType, DailyCount[]>();
  for (const row of dailyCounts) {
    const list = byNeed.get(row.needType) ?? [];
    list.push(row);
    byNeed.set(row.needType, list);
  }
  const results: NeedTypeForecast[] = [];
  for (const [needType, rows] of byNeed) {
    const sorted = [...rows].sort((a, b) => a.day.localeCompare(b.day));
    const n = sorted.length;
    const xs = sorted.map((_, i) => i);
    const ys = sorted.map((r) => r.count);
    const xMean = xs.reduce((a, b) => a + b, 0) / n;
    const yMean = ys.reduce((a, b) => a + b, 0) / n;
    const num = xs.reduce((acc, x, i) => acc + (x - xMean) * (ys[i] - yMean), 0);
    const den = xs.reduce((acc, x) => acc + (x - xMean) ** 2, 0) || 1;
    const slope = num / den;
    const intercept = yMean - slope * xMean;
    const lastDay = new Date(sorted[n - 1].day);
    const forecast: ForecastPoint[] = Array.from({ length: horizonDays }, (_, i) => {
      const d = new Date(lastDay);
      d.setDate(d.getDate() + i + 1);
      return { day: d.toISOString().slice(0, 10), projectedCount: Math.max(0, Math.round(slope * (n - 1 + i + 1) + intercept)) };
    });
    const trend = slope > 0.1 ? "rising" : slope < -0.1 ? "falling" : "flat";
    results.push({ needType, history: sorted, forecast, trend });
  }
  return results;
}

export function timeAgo(iso: string) {
  const m = Math.max(1, Math.round((Date.now() - new Date(iso).getTime()) / 60000));
  return m < 60 ? `${m} min ago` : m < 1440 ? `${Math.round(m / 60)} h ago` : `${Math.round(m / 1440)} d ago`;
}
