"""
Demand forecasting (Phase 3): given recent daily request counts per need type, project
the next few days forward.

Deliberately simple — linear regression over a 1D day index, per need type — rather than
a heavier time-series model (ARIMA, Prophet). With only a few weeks of real history this
project will have for a while, a simple trend line is honest about its own uncertainty and
easy to sanity-check by eye; a more sophisticated model would mostly be fitting noise.
Swap this out once there's enough history (a few months) for seasonality to matter.
"""
from collections import defaultdict
from datetime import date, timedelta

import numpy as np
from pydantic import BaseModel


class DailyCount(BaseModel):
    day: date
    needType: str
    count: int


class ForecastPoint(BaseModel):
    day: date
    projectedCount: float


class NeedTypeForecast(BaseModel):
    needType: str
    history: list[DailyCount]
    forecast: list[ForecastPoint]
    trend: str  # "rising" | "falling" | "flat"


def forecast(daily_counts: list[DailyCount], horizon_days: int = 7) -> list[NeedTypeForecast]:
    by_need: dict[str, list[DailyCount]] = defaultdict(list)
    for dc in daily_counts:
        by_need[dc.needType].append(dc)

    results: list[NeedTypeForecast] = []
    for need_type, rows in by_need.items():
        rows.sort(key=lambda r: r.day)
        results.append(_forecast_one(need_type, rows, horizon_days))
    return results


def _forecast_one(need_type: str, rows: list[DailyCount], horizon_days: int) -> NeedTypeForecast:
    if len(rows) < 2:
        # Not enough history for a trend line — project the one data point flat forward
        # rather than pretending a slope can be fit from a single observation.
        last_day = rows[-1].day if rows else date.today()
        last_count = float(rows[-1].count) if rows else 0.0
        flat = [
            ForecastPoint(day=last_day + timedelta(days=i), projectedCount=last_count)
            for i in range(1, horizon_days + 1)
        ]
        return NeedTypeForecast(needType=need_type, history=rows, forecast=flat, trend="flat")

    x = np.arange(len(rows), dtype=float)
    y = np.array([r.count for r in rows], dtype=float)
    slope, intercept = np.polyfit(x, y, 1)

    last_day = rows[-1].day
    forecast_points = [
        ForecastPoint(
            day=last_day + timedelta(days=i),
            projectedCount=max(0.0, round(float(slope * (len(rows) - 1 + i) + intercept), 2)),
        )
        for i in range(1, horizon_days + 1)
    ]

    # A flat threshold rather than a statistical significance test — this is a direction
    # indicator for a dashboard, not a claim that the trend is statistically proven.
    if slope > 0.1:
        trend = "rising"
    elif slope < -0.1:
        trend = "falling"
    else:
        trend = "flat"

    return NeedTypeForecast(needType=need_type, history=rows, forecast=forecast_points, trend=trend)
