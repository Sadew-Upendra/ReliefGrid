"use client";
import { useEffect, useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { ArrowLeft, TrendingDown, TrendingUp, Minus } from "lucide-react";
import {
  CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from "recharts";
import { Link } from "@/i18n/navigation";
import LanguageSwitcher from "@/components/LanguageSwitcher";
import RequireAuth from "@/components/RequireAuth";
import { NeedTypeForecast, URGENCY_COLOR, getDailyCounts, getForecast } from "@/lib/api";

const CHART_COLORS: Record<string, string> = {
  FOOD: URGENCY_COLOR.HIGH, WATER: "#2E86AB", MEDICINE: URGENCY_COLOR.CRITICAL,
  SHELTER: URGENCY_COLOR.MEDIUM, RESCUE: "#6B4FA0", OTHER: "#6B7280",
};

const TREND_ICON = { rising: TrendingUp, falling: TrendingDown, flat: Minus } as const;

function ForecastContent() {
  const t = useTranslations("forecast");
  const tNeeds = useTranslations("needs");
  const [days, setDays] = useState(30);
  const [data, setData] = useState<NeedTypeForecast[] | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setData(null);
    setFailed(false);
    getDailyCounts(days)
      .then((counts) => getForecast(counts, 7))
      .then((result) => { if (!cancelled) setData(result); })
      .catch(() => { if (!cancelled) setFailed(true); });
    return () => { cancelled = true; };
  }, [days]);

  type ChartRow = { day: string; actual: number | null; projected: number | null };

  const chartsData = useMemo(() => {
    if (!data) return [];
    return data.map((series) => {
      const rows: ChartRow[] = [
        ...series.history.map((h) => ({ day: h.day, actual: h.count, projected: null })),
        ...series.forecast.map((f) => ({ day: f.day, actual: null, projected: f.projectedCount })),
      ];
      // Bridge the gap so the projected line visually connects to the last actual point.
      if (series.history.length > 0 && series.forecast.length > 0) {
        const lastActualIdx = series.history.length - 1;
        rows[lastActualIdx] = { ...rows[lastActualIdx], projected: rows[lastActualIdx].actual };
      }
      return { series, rows };
    });
  }, [data]);

  return (
    <main className="mx-auto max-w-4xl px-6 py-10">
      <header className="mb-6 flex items-center justify-between">
        <Link href="/dashboard" className="inline-flex items-center gap-1.5 text-sm font-medium text-brand hover:text-brand-dark">
          <ArrowLeft size={15} /> {t("backToDashboard")}
        </Link>
        <LanguageSwitcher />
      </header>

      <h1 className="font-display text-3xl font-bold tracking-tight">{t("title")}</h1>
      <p className="mt-2 max-w-xl text-ink/70">{t("subtitle")}</p>

      <label className="mt-6 flex items-center gap-3 text-sm">
        {t("days")}
        <input
          type="range" min={7} max={60} step={1} value={days}
          onChange={(e) => setDays(Number(e.target.value))}
          className="w-48 accent-brand"
        />
        <span className="font-medium">{days}d</span>
      </label>

      {failed && <p className="mt-8 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800">{t("loadError")}</p>}

      {!failed && data === null && (
        <div className="mt-8 space-y-4">
          {[1, 2, 3].map((i) => <div key={i} className="h-40 animate-pulse rounded-xl bg-paper" />)}
        </div>
      )}

      {!failed && data !== null && data.length === 0 && (
        <p className="mt-8 text-sm text-ink/60">{t("noData")}</p>
      )}

      <div className="mt-8 grid grid-cols-1 gap-6 md:grid-cols-2">
        {chartsData.map(({ series, rows }) => {
          const TrendIcon = TREND_ICON[series.trend];
          const color = CHART_COLORS[series.needType] ?? "#0E7C7B";
          return (
            <div key={series.needType} className="rounded-xl border border-line bg-white p-4">
              <div className="mb-2 flex items-center justify-between">
                <h2 className="font-display text-base font-bold">{tNeeds(series.needType)}</h2>
                <span className="inline-flex items-center gap-1 text-xs font-medium text-ink/60">
                  <TrendIcon size={14} /> {t(`trend${series.trend[0].toUpperCase()}${series.trend.slice(1)}`)}
                </span>
              </div>
              <ResponsiveContainer width="100%" height={160}>
                <LineChart data={rows} margin={{ top: 4, right: 8, left: -24, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#DCE3E0" />
                  <XAxis dataKey="day" tick={{ fontSize: 10 }} tickFormatter={(d: string) => d.slice(5)} />
                  <YAxis tick={{ fontSize: 10 }} allowDecimals={false} />
                  <Tooltip labelFormatter={(d) => d} />
                  <Line type="monotone" dataKey="actual" name={t("historyLegend")} stroke={color} strokeWidth={2} dot={false} connectNulls={false} />
                  <Line type="monotone" dataKey="projected" name={t("forecastLegend")} stroke={color} strokeWidth={2} strokeDasharray="4 3" dot={false} connectNulls />
                </LineChart>
              </ResponsiveContainer>
            </div>
          );
        })}
      </div>
    </main>
  );
}

export default function ForecastPage() {
  return (
    <RequireAuth allowedRoles={["COORDINATOR", "ADMIN"]}>
      <ForecastContent />
    </RequireAuth>
  );
}
