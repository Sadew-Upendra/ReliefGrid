"use client";
import { Suspense, useCallback, useEffect, useMemo, useState } from "react";
import dynamic from "next/dynamic";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { Check, MapPin, Plus, X } from "lucide-react";
import { Link } from "@/i18n/navigation";
import LanguageSwitcher from "@/components/LanguageSwitcher";
import OfflineBanner from "@/components/OfflineBanner";
import RequireAuth from "@/components/RequireAuth";
import UserMenu from "@/components/UserMenu";
import { useAuth } from "@/context/AuthContext";
import {
  NEED_TYPES, NeedType, ReliefRequest, Status, URGENCY_COLOR, URGENCY_LEVELS, Urgency,
  createRequest, listRequests, timeAgo, updateStatus,
} from "@/lib/api";
import Logo from "@/components/Logo";

const ReliefMap = dynamic(() => import("@/components/ReliefMap"), { ssr: false, loading: () => <div className="h-full w-full animate-pulse bg-line" /> });

function Dashboard() {
  const t = useTranslations("dashboard");
  const tForecast = useTranslations("forecast");
  const tNeeds = useTranslations("needs");
  const { user } = useAuth();
  const canModerate = user?.role === "COORDINATOR" || user?.role === "ADMIN";
  const tUrgency = useTranslations("urgency");
  const tStatus = useTranslations("requestStatus");

  const params = useSearchParams();
  const [items, setItems] = useState<ReliefRequest[] | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const [filter, setFilter] = useState<Urgency | "ALL">("ALL");
  const [form, setForm] = useState(params.get("report") === "1");
  const [pin, setPin] = useState<[number, number] | null>(null);
  const [need, setNeed] = useState<NeedType>("FOOD");
  const [qty, setQty] = useState(10);
  const [desc, setDesc] = useState("");
  const [risk, setRisk] = useState(false);
  const [busy, setBusy] = useState(false);
  const [toast, setToast] = useState("");

  useEffect(() => { listRequests().then(setItems); }, []);
  const shown = useMemo(
    () => (items ?? []).filter((r) => filter === "ALL" || r.urgency === filter).sort((a, b) => URGENCY_LEVELS.indexOf(a.urgency) - URGENCY_LEVELS.indexOf(b.urgency) || +new Date(b.createdAt) - +new Date(a.createdAt)),
    [items, filter],
  );
  const current = items?.find((r) => r.id === selected);
  const onSelect = useCallback((id: string) => setSelected(id), []);
  const say = (m: string) => { setToast(m); setTimeout(() => setToast(""), 3000); };

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!pin) return;
    setBusy(true);
    const r = await createRequest({ needType: need, quantity: qty, description: desc, lat: pin[1], lng: pin[0], lifeAtRisk: risk });
    setItems((p) => [r, ...(p ?? [])]);
    setSelected(r.id); setForm(false); setPin(null); setDesc(""); setRisk(false); setBusy(false);
    say(t("requestSentToast"));
  }
  async function setStatus(id: string, s: Status) {
    await updateStatus(id, s);
    setItems((p) => (p ?? []).map((r) => (r.id === id ? { ...r, status: s } : r)));
    say(s === "VERIFIED" ? t("markedVerifiedToast") : t("markedFulfilledToast"));
  }

  return (
    <div className="flex h-screen flex-col">
      <OfflineBanner />
      <header className="flex items-center justify-between border-b border-line bg-white px-5 py-3">
        <Logo height={32} />
        <div className="flex items-center gap-3">
          {canModerate && (
            <Link href="/forecast" className="hidden text-sm font-medium text-ink/70 hover:text-ink sm:inline">
              {tForecast("title")}
            </Link>
          )}
          <LanguageSwitcher />
          <button onClick={() => { setForm(true); setSelected(null); }} className="inline-flex items-center gap-2 rounded-lg bg-brand px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-dark">
            <Plus size={16} /> {t("reportNeed")}
          </button>
          <UserMenu />
        </div>
      </header>

      <div className="grid min-h-0 flex-1 grid-rows-[45%_55%] md:grid-cols-[400px_1fr] md:grid-rows-1">
        <aside className="order-2 flex min-h-0 flex-col border-line bg-white md:order-1 md:border-r">
          {form ? (
            <form onSubmit={submit} className="flex-1 space-y-5 overflow-y-auto p-5">
              <div className="flex items-center justify-between">
                <h2 className="font-display text-xl font-bold">{t("reportNeed")}</h2>
                <button type="button" aria-label={t("closeForm")} onClick={() => { setForm(false); setPin(null); }} className="rounded p-1 hover:bg-paper"><X size={18} /></button>
              </div>
              <fieldset>
                <legend className="mb-2 text-sm font-medium">{t("whatIsNeeded")}</legend>
                <div className="flex flex-wrap gap-2">
                  {NEED_TYPES.map((n) => (
                    <button type="button" key={n} onClick={() => setNeed(n)} aria-pressed={need === n}
                      className={`rounded-full border px-3.5 py-1.5 text-sm transition ${need === n ? "border-brand bg-brand text-white" : "border-line hover:border-brand"}`}>{tNeeds(n)}</button>
                  ))}
                </div>
              </fieldset>
              <label className="block text-sm font-medium">{t("howMany")}
                <input type="number" min={1} value={qty} onChange={(e) => setQty(Math.max(1, +e.target.value))} className="mt-2 w-full rounded-lg border border-line px-3 py-2 font-normal" />
              </label>
              <label className="block text-sm font-medium">{t("whatIsHappening")}
                <textarea required maxLength={500} rows={3} value={desc} onChange={(e) => setDesc(e.target.value)} placeholder={t("whatIsHappeningPlaceholder")} className="mt-2 w-full rounded-lg border border-line px-3 py-2 font-normal" />
              </label>
              <label className="flex items-center gap-3 rounded-lg bg-red-50 px-3 py-3 text-sm">
                <input type="checkbox" checked={risk} onChange={(e) => setRisk(e.target.checked)} className="h-4 w-4 accent-red-600" />
                {t("lifeAtRisk")}
              </label>
              <div className={`flex items-center gap-2 rounded-lg border border-dashed px-3 py-3 text-sm ${pin ? "border-brand bg-brand/5" : "border-line"}`}>
                <MapPin size={16} className="text-brand" />
                {pin ? t("locationSetMoveHint") : t("tapMapToPlacePin")}
              </div>
              <button disabled={!pin || busy} className="w-full rounded-lg bg-brand py-3 font-medium text-white transition hover:bg-brand-dark disabled:opacity-40">
                {busy ? t("sending") : t("send")}
              </button>
            </form>
          ) : (
            <>
              <div className="flex gap-2 overflow-x-auto border-b border-line p-3">
                {(["ALL", ...URGENCY_LEVELS] as const).map((u) => (
                  <button key={u} onClick={() => setFilter(u)} aria-pressed={filter === u}
                    className={`shrink-0 rounded-full border px-3.5 py-1.5 text-sm transition ${filter === u ? "border-ink bg-ink text-white" : "border-line hover:border-ink"}`}>
                    {u === "ALL" ? t("all") : tUrgency(u)}
                  </button>
                ))}
              </div>
              <ul className="flex-1 overflow-y-auto">
                {items === null && [1, 2, 3, 4].map((i) => <li key={i} className="m-3 h-16 animate-pulse rounded-lg bg-paper" />)}
                {items && shown.length === 0 && <li className="p-6 text-center text-sm text-ink/60">{t("noRequestsMatch")}</li>}
                {shown.map((r) => (
                  <li key={r.id}>
                    <button onClick={() => setSelected(r.id)} className={`flex w-full gap-3 border-b border-line px-4 py-3.5 text-left transition hover:bg-paper ${r.id === selected ? "bg-paper" : ""}`}>
                      <span className="mt-1.5 h-3 w-3 shrink-0 rounded-full" style={{ background: URGENCY_COLOR[r.urgency] }} />
                      <span className="min-w-0 flex-1">
                        <span className="flex justify-between gap-2 font-medium"><span>{tNeeds(r.needType)} · {r.quantity}</span><span className="text-xs font-normal text-ink/50">{timeAgo(r.createdAt)}</span></span>
                        <span className="block truncate text-sm text-ink/60">{r.place}</span>
                        <span className="mt-1 block text-xs text-ink/50">{tStatus(r.status)}</span>
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            </>
          )}
        </aside>

        <section className="relative order-1 md:order-2">
          <ReliefMap items={items ?? []} selectedId={selected} onSelect={onSelect} picking={form} pin={pin} onPick={setPin} />
          {current && !form && (
            <div className="absolute inset-x-3 bottom-3 rounded-xl border border-line bg-white p-4 shadow-lg md:left-4 md:right-auto md:w-96">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="font-display text-lg font-bold">{tNeeds(current.needType)} · {current.quantity}</p>
                  <p className="text-sm text-ink/60">{current.place} · {tUrgency(current.urgency)}</p>
                </div>
                <button aria-label={t("closeDetails")} onClick={() => setSelected(null)} className="rounded p-1 hover:bg-paper"><X size={16} /></button>
              </div>
              <p className="mt-2 text-sm leading-relaxed">{current.description}</p>
              <div className="mt-3 flex items-center justify-between gap-2">
                <span className="text-sm font-medium text-brand">{tStatus(current.status)}</span>
                {canModerate && current.status === "PENDING" && <button onClick={() => setStatus(current.id, "VERIFIED")} className="rounded-lg border border-brand px-3 py-1.5 text-sm font-medium text-brand hover:bg-brand hover:text-white">{t("verify")}</button>}
                {canModerate && (current.status === "VERIFIED" || current.status === "ASSIGNED") && <button onClick={() => setStatus(current.id, "FULFILLED")} className="rounded-lg border border-brand px-3 py-1.5 text-sm font-medium text-brand hover:bg-brand hover:text-white">{t("markFulfilled")}</button>}
              </div>
            </div>
          )}
          {toast && <div role="status" className="absolute left-1/2 top-4 flex -translate-x-1/2 items-center gap-2 rounded-lg bg-ink px-4 py-2.5 text-sm text-white shadow-lg"><Check size={16} />{toast}</div>}
        </section>
      </div>
    </div>
  );
}

export default function Page() {
  return (
    <RequireAuth>
      <Suspense><Dashboard /></Suspense>
    </RequireAuth>
  );
}
