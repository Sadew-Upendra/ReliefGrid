"use client";
import { useTranslations } from "next-intl";
import { LogOut } from "lucide-react";
import { useRouter } from "@/i18n/navigation";
import { useAuth } from "@/context/AuthContext";

export default function UserMenu() {
  const t = useTranslations("auth");
  const { user, logout } = useAuth();
  const router = useRouter();

  if (!user) return null;

  async function handleLogout() {
    await logout();
    router.replace("/");
  }

  return (
    <div className="flex items-center gap-2 text-sm">
      <span className="hidden text-ink/70 md:inline">{user!.name}</span>
      <button onClick={handleLogout} aria-label={t("logout")} title={t("logout")}
        className="rounded-lg p-2 text-ink/60 hover:bg-paper hover:text-ink">
        <LogOut size={16} />
      </button>
    </div>
  );
}
