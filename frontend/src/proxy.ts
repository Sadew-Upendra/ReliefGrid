import createMiddleware from "next-intl/middleware";
import { routing } from "./i18n/routing";

export default createMiddleware(routing);

export const config = {
  // Skip static files, Next.js internals, the PWA manifest/service worker, and API routes.
  matcher: ["/((?!api|_next|_vercel|sw.js|manifest.webmanifest|icons|.*\\..*).*)"],
};
