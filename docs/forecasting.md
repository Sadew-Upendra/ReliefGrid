# ReliefGrid — Demand Forecasting (Phase 3)

## What it does
Given recent daily request counts per need type, projects the next few days forward per
need type, with a simple "rising / falling / flat" trend label. Meant for a coordinator
dashboard: "FOOD requests have been rising for the last week — expect ~12/day by Friday."

## Why a simple linear trend, not a heavier time-series model
With at most a few weeks of real history for a while, a linear regression per need type is
honest about its own uncertainty and easy to sanity-check by eye. A more sophisticated model
(ARIMA, Prophet, seasonal decomposition) would mostly be fitting noise at this data volume —
worth revisiting once there's enough history (a few months) for weekly/seasonal patterns to
show up. See the comment at the top of `ai-service/app/forecasting/forecaster.py`.

## How the pieces fit together
```
Coordinator dashboard (not yet built — see "Still to do" below)
        │
        ▼
GET /api/v1/requests/daily-counts?days=30     (backend, coordinator/admin only)
        │  [{day, needType, count}, ...]
        ▼
POST /forecast  { dailyCounts, horizonDays }   (ai-service)
        │  [{needType, history, forecast, trend}, ...]
        ▼
Rendered as a small line chart per need type
```
The backend endpoint and the AI service endpoint are intentionally not wired directly to
each other — a browser (or a scheduled job) calls the first, then passes its result to the
second. That keeps the AI service stateless and keeps "when to refresh the forecast" a
frontend/ops decision, not something baked into the backend.

## Try it without the frontend
```bash
# 1. Get real daily counts (needs a logged-in coordinator/admin JWT)
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/requests/daily-counts?days=30"

# 2. Feed them to the forecaster
curl -X POST http://localhost:8000/forecast \
  -H "Content-Type: application/json" \
  -d '{"dailyCounts": [...output from step 1...], "horizonDays": 7}'
```

## The chart
`frontend/src/app/[locale]/forecast/page.tsx` — calls both endpoints in sequence and
renders a small per-need-type line chart (recharts), solid line for actual history, dashed
for the projection, with a rising/falling/flat badge. Linked from the dashboard header.

Falls back to a client-side linear fit over synthetic demo data when
`NEXT_PUBLIC_AI_SERVICE_URL` isn't set, same demo-mode pattern as the rest of the frontend.

Gated client-side to COORDINATOR/ADMIN via `RequireAuth` (see `docs/frontend-auth.md`) — a
UX convenience on top of, not a substitute for, the backend's own `@PreAuthorize` check on
`/requests/daily-counts`.
