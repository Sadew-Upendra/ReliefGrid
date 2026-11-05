import { ArrowLeft } from "lucide-react";
import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import LanguageSwitcher from "@/components/LanguageSwitcher";
import { NEED_TYPES, URGENCY_COLOR, URGENCY_LEVELS, getPublicStatus } from "@/lib/api";

// Re-fetched on every request — this is a live status page, not a static one.
export const dynamic = "force-dynamic";

export default async function StatusPage() {
  const t = await getTranslations("status");
  const tNeeds = await getTranslations("needs");
  const tUrgency = await getTranslations("urgency");

  let status: Awaited<ReturnType<typeof getPublicStatus>> | null = null;
  let loadFailed = false;
  try {
    status = await getPublicStatus();
  } catch {
    loadFailed = true;
  }

  return (
    <main className="mx-auto max-w-3xl px-6 py-10">
      <header className="mb-8 flex items-center justify-between">
        <Link href="/" className="inline-flex items-center gap-1.5 text-sm font-medium text-brand hover:text-brand-dark">
          <ArrowLeft size={15} /> {t("backHome")}
        </Link>
        <LanguageSwitcher />
      </header>

      <h1 className="font-display text-3xl font-bold tracking-tight">{t("title")}</h1>
      <p className="mt-2 max-w-xl text-ink/70">{t("subtitle")}</p>

      {loadFailed || !status ? (
        <p className="mt-10 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800">{t("loadError")}</p>
      ) : (
        <>
          <div className="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-3">
            <Stat label={t("openRequests")} value={status.openRequests} />
            <Stat label={t("fulfilledRequests")} value={status.fulfilledRequests} />
            <Stat label={t("activeVolunteerTasks")} value={status.activeVolunteerTasks} />
          </div>

          <section className="mt-10">
            <h2 className="mb-3 font-display text-lg font-bold">{t("byUrgency")}</h2>
            <div className="space-y-2">
              {URGENCY_LEVELS.map((u) => {
                const value = status.openByUrgency[u] ?? 0;
                const max = Math.max(1, ...URGENCY_LEVELS.map((x) => status.openByUrgency[x] ?? 0));
                return (
                  <div key={u} className="flex items-center gap-3">
                    <span className="w-20 shrink-0 text-sm text-ink/70">{tUrgency(u)}</span>
                    <div className="h-3 flex-1 overflow-hidden rounded-full bg-paper">
                      <div className="h-full rounded-full" style={{ width: `${(value / max) * 100}%`, background: URGENCY_COLOR[u] }} />
                    </div>
                    <span className="w-6 shrink-0 text-right text-sm font-medium">{value}</span>
                  </div>
                );
              })}
            </div>
          </section>

          <section className="mt-10">
            <h2 className="mb-3 font-display text-lg font-bold">{t("byNeedType")}</h2>
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
              {NEED_TYPES.map((n) => (
                <div key={n} className="rounded-lg border border-line bg-white p-3 text-center">
                  <p className="text-2xl font-bold">{status.openByNeedType[n] ?? 0}</p>
                  <p className="text-xs text-ink/60">{tNeeds(n)}</p>
                </div>
              ))}
            </div>
          </section>

          <p className="mt-10 text-xs text-ink/50">
            {t("generatedAt")}: {new Date(status.generatedAt).toLocaleString()}
          </p>
        </>
      )}
    </main>
  );
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-xl border border-line bg-white p-5">
      <p className="font-display text-3xl font-bold">{value}</p>
      <p className="mt-1 text-sm text-ink/60">{label}</p>
    </div>
  );
}
