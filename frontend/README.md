# ReliefGrid Frontend

Next.js 14 + TypeScript + Tailwind + MapLibre, with multi-language support (next-intl) and
basic offline/PWA support.

```bash
npm install
npm run dev        # http://localhost:3000 -> redirects to /en
```

Runs on built-in demo data. To use the real backend, copy `.env.example` to `.env.local` and set `NEXT_PUBLIC_API_URL`. All API calls live in `src/lib/api.ts`.

## Languages (Phase 3)
English, Sinhala (සිංහල), and Tamil (தமிழ்) — see `messages/{en,si,ta}.json`. Routes are
locale-prefixed (`/en/dashboard`, `/si/dashboard`, `/ta/dashboard`); the middleware
(`src/middleware.ts`) redirects `/` to the visitor's preferred language automatically.
The Sinhala and Tamil translations were written with AI assistance — worth a native
speaker's review before this goes in front of real users in those languages.

To add a language: add its locale code to `src/i18n/routing.ts`, add `messages/<code>.json`
with the same keys as `messages/en.json`, and add a label for it in `LanguageSwitcher.tsx`.

## Offline / PWA (Phase 3)
`public/manifest.webmanifest` + a hand-written service worker (`public/sw.js`, registered by
`src/components/ServiceWorkerRegister.tsx`) cache the app shell, so a previously-opened
dashboard still renders when offline. This is deliberately limited: API calls are never
cached (stale relief data is worse than an error), and `OfflineBanner.tsx` tells the user
plainly that new reports can't be sent until they're back online — it does not claim full
offline functionality.

## Authentication
Real login/register, wired to the backend's JWT endpoints — see `docs/frontend-auth.md` for
the full picture, including an honest note on its one known weakness: tokens are kept in
`localStorage`, not an `httpOnly` cookie, which is fine for this prototype but not for a real
deployment facing the public internet.

## Public status page (Phase 3)
`/status` (e.g. `/en/status`) — a public, no-auth page showing aggregate counts only (open
requests by urgency/need type, fulfilled count, active volunteer tasks). Reads
`GET /api/v1/public/status` from the backend. No names, locations, or descriptions are ever
shown here — see the backend's `PublicStatusController`.
