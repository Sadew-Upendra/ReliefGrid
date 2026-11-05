"use client";
import { useLocale, useTranslations } from "next-intl";
import { routing } from "@/i18n/routing";
import { usePathname, useRouter } from "@/i18n/navigation";

const LABELS: Record<string, string> = { en: "English", si: "\u0dc3\u0dd2\u0d82\u0dc4\u0dbd", ta: "\u0ba4\u0bae\u0bbf\u0bb4\u0bcd" };

export default function LanguageSwitcher() {
  const t = useTranslations("nav");
  const locale = useLocale();
  const router = useRouter();
  const pathname = usePathname();

  return (
    <label className="flex items-center gap-1.5 text-sm text-ink/70">
      <span className="sr-only">{t("language")}</span>
      <select
        value={locale}
        onChange={(e) => router.replace(pathname, { locale: e.target.value })}
        className="rounded-md border border-line bg-white px-2 py-1 text-sm"
        aria-label={t("language")}
      >
        {routing.locales.map((l) => (
          <option key={l} value={l}>
            {LABELS[l] ?? l}
          </option>
        ))}
      </select>
    </label>
  );
}
