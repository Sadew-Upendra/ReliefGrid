"use client";
import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import { useAuth } from "@/context/AuthContext";
import UserMenu from "@/components/UserMenu";

/** Client component so it can read auth state; kept small and separate so the landing
 * page itself can stay a server component (faster first paint, no client JS needed
 * for the static marketing content around it). */
export default function NavAuthLinks() {
  const t = useTranslations("auth");
  const { user, loading } = useAuth();

  if (loading) return null;

  if (!user) {
    return (
      <div className="flex items-center gap-3 text-sm font-medium">
        <Link href="/login" className="text-ink/70 hover:text-ink">{t("login")}</Link>
        <Link href="/register" className="text-brand hover:text-brand-dark">{t("register")}</Link>
      </div>
    );
  }

  return <UserMenu />;
}
