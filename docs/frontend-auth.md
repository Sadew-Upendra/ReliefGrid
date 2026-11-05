# ReliefGrid — Frontend Authentication

## What's here
- `src/lib/tokenStore.ts` — holds the access/refresh tokens + `{name, role}`, backed by
  `localStorage` so a session survives a page reload.
- `src/lib/api.ts` — `login`, `register`, `restoreSession`, `logout`, and an internal
  `authFetch` wrapper that attaches `Authorization: Bearer <token>` to every protected call
  and retries once, silently, after a token refresh if the first attempt gets a 401.
- `src/context/AuthContext.tsx` — `AuthProvider` (wraps the whole app in
  `[locale]/layout.tsx`) and the `useAuth()` hook (`user`, `loading`, `login`, `register`, `logout`).
- `src/components/RequireAuth.tsx` — wraps a page; redirects to `/login?next=<path>` if
  signed out, and can further restrict to specific roles (`allowedRoles`).
- `/login` and `/register` pages, a `UserMenu` (name + logout), and a `NavAuthLinks`
  component on the landing page that shows login/register links when signed out.

## Where it's applied
- `/dashboard` — requires any signed-in user (matches the backend: any role can submit a
  request). The Verify/Mark fulfilled buttons are additionally hidden client-side unless
  the user is COORDINATOR or ADMIN.
- `/forecast` — requires COORDINATOR or ADMIN.
- `/status` and the landing page — stay public, no guard, matching the backend's own
  `permitAll` on `/api/v1/public/**`.

All of this is a UX convenience layered on top of, never a substitute for, the backend's
own `@PreAuthorize` checks — a client-side guard can always be bypassed by calling the API
directly; the backend is the actual access control.

## Security trade-off, stated plainly
Tokens live in `localStorage`, readable by any script running on the page — a real XSS risk.
This is the simplest thing that works for a prototype with no backend-for-frontend layer. A
production deployment should instead have a server-side route (or the backend itself) set the
refresh token as an `httpOnly`, `SameSite=Strict` cookie, with only a short-lived access token
ever reaching client-side JavaScript. Noted here and in `frontend/README.md` rather than left
implicit.

## Incidental bug fixed along the way
`listRequests()` was parsing the backend's response as a flat array, but the backend wraps
list endpoints in a `PageResponse<T>` (`{content, page, size, totalElements, totalPages}`) —
see `backend`'s `RequestController`. Fixed to read `.content`. This had gone unnoticed because
the frontend had never actually been wired to call the real backend with a valid token before.
