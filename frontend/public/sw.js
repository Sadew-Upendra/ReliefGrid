// Minimal, hand-written service worker — no next-pwa or Workbox dependency.
// Goal: the app shell (and the dashboard's already-loaded request list) still opens
// when offline; it is NOT a claim that new data can be fetched or submitted offline —
// see OfflineBanner.tsx for how the UI is honest about that distinction.
const CACHE_NAME = "reliefgrid-shell-v1";
// Deliberately just "/" (the locale-detection redirect) plus static assets — not
// "/dashboard", since real routes are locale-prefixed (/en/dashboard, /si/dashboard, ...)
// and which one a given visitor lands on depends on their browser language. The runtime
// fetch handler below caches whichever locale route they actually visit, on first load.
const SHELL_URLS = ["/", "/manifest.webmanifest", "/icons/icon-192.png"];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(SHELL_URLS)).catch(() => {})
  );
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k)))
    )
  );
  self.clients.claim();
});

self.addEventListener("fetch", (event) => {
  const { request } = event;

  // Never cache API calls — stale request data is worse than a clear network error.
  if (request.url.includes("/api/")) return;
  if (request.method !== "GET") return;

  event.respondWith(
    caches.match(request).then((cached) => {
      const network = fetch(request)
        .then((response) => {
          if (response && response.status === 200) {
            const copy = response.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(request, copy));
          }
          return response;
        })
        .catch(() => cached);
      return cached || network;
    })
  );
});
