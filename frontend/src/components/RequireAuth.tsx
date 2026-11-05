"use client";
import { useEffect } from "react";
import { useTranslations } from "next-intl";
import { usePathname, useRouter } from "@/i18n/navigation";
import { useAuth } from "@/context/AuthContext";
import type { Role } from "@/lib/api";

/** Wrap any page that needs a signed-in user. Optionally restrict to specific roles —
 * used by the forecast page, which the backend itself also enforces server-side
 * (this is a UX convenience, not the actual access control). */
export default function RequireAuth({ children, allowedRoles }: { children: React.ReactNode; allowedRoles?: Role[] }) {
  const t = useTranslations("auth");
  const { user, loading } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!loading && !user) {
      router.replace(`/login?next=${encodeURIComponent(pathname)}`);
    }
  }, [loading, user, router, pathname]);

  if (loading || !user) {
    return <p className="p-10 text-center text-sm text-ink/60">{t("redirecting")}</p>;
  }
  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <p className="p-10 text-center text-sm text-ink/60">{t("signInRequired")}</p>;
  }
  return <>{children}</>;
}
