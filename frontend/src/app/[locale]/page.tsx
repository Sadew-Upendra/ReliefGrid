import { ArrowRight, MapPin } from "lucide-react";
import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import LanguageSwitcher from "@/components/LanguageSwitcher";
import Logo from "@/components/Logo";
import NavAuthLinks from "@/components/NavAuthLinks";
import { SEED, URGENCY_COLOR } from "@/lib/api";

export default async function Home() {
  const t = await getTranslations();

  return (
    <main className="mx-auto flex min-h-screen max-w-6xl flex-col px-6">
      <header className="flex items-center justify-between py-6">
        <Logo height={40} priority />
        <div className="flex items-center gap-4">
          <Link href="/status" className="text-sm font-medium text-ink/70 hover:text-ink">
            {t("nav.liveStatus")}
          </Link>
          <Link href="/dashboard" className="text-sm font-medium text-brand hover:text-brand-dark">
            {t("nav.openLiveMap")}
          </Link>
          <LanguageSwitcher />
          <NavAuthLinks />
        </div>
      </header>

      <section className="grid flex-1 items-center gap-12 py-10 md:grid-cols-2">
        <div>
          <h1 className="font-display text-5xl font-bold leading-[1.05] tracking-tight md:text-6xl">
            {t("landing.title")}
          </h1>
          <p className="mt-6 max-w-md text-lg leading-relaxed text-ink/70">{t("landing.subtitle")}</p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link href="/dashboard" className="inline-flex items-center gap-2 rounded-lg bg-brand px-5 py-3 font-medium text-white transition hover:bg-brand-dark">
              {t("landing.openLiveMap")} <ArrowRight size={18} />
            </Link>
            <Link href="/dashboard?report=1" className="rounded-lg border border-line bg-white px-5 py-3 font-medium transition hover:border-brand">
              {t("landing.reportNeed")}
            </Link>
          </div>
        </div>

        <div className="rounded-2xl border border-line bg-white p-2 shadow-sm">
          <p className="px-4 pb-1 pt-3 text-sm font-medium text-ink/60">{t("landing.rightNowNear")}</p>
          <ul>
            {SEED.slice(0, 4).map((r) => (
              <li key={r.id} className="flex items-center gap-4 rounded-xl px-4 py-3">
                <span className="h-3.5 w-3.5 shrink-0 rounded-full" style={{ background: URGENCY_COLOR[r.urgency] }} />
                <div className="min-w-0 flex-1">
                  <p className="font-medium">
                    {t(`needs.${r.needType}`)} · {r.quantity}
                  </p>
                  <p className="flex items-center gap-1 truncate text-sm text-ink/60">
                    <MapPin size={13} />
                    {r.place}
                  </p>
                </div>
                <span className="text-sm text-ink/60">{t(`urgency.${r.urgency}`)}</span>
              </li>
            ))}
          </ul>
        </div>
      </section>
    </main>
  );
}
