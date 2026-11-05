"use client";
import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { WifiOff } from "lucide-react";

/** Honest about what "offline" means here: the last-loaded map/list stays visible
 * (service worker caches the app shell), but nothing new can be sent — see public/sw.js. */
export default function OfflineBanner() {
  const t = useTranslations("dashboard");
  const [isOffline, setIsOffline] = useState(false);

  useEffect(() => {
    setIsOffline(!navigator.onLine);
    const goOffline = () => setIsOffline(true);
    const goOnline = () => setIsOffline(false);
    window.addEventListener("offline", goOffline);
    window.addEventListener("online", goOnline);
    return () => {
      window.removeEventListener("offline", goOffline);
      window.removeEventListener("online", goOnline);
    };
  }, []);

  if (!isOffline) return null;

  return (
    <div role="status" className="flex items-center gap-2 border-b border-amber-200 bg-amber-50 px-4 py-2 text-sm text-amber-900">
      <WifiOff size={15} className="shrink-0" />
      {t("offlineBanner")}
    </div>
  );
}
