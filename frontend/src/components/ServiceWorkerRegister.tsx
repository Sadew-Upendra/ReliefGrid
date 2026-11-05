"use client";
import { useEffect } from "react";

/** Registers the hand-written service worker (public/sw.js) once, client-side only. */
export default function ServiceWorkerRegister() {
  useEffect(() => {
    if ("serviceWorker" in navigator) {
      navigator.serviceWorker.register("/sw.js").catch(() => {
        // Offline support is a nice-to-have, not a hard requirement — fail silently.
      });
    }
  }, []);
  return null;
}
