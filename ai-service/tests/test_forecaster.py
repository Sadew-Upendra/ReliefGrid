from datetime import date, timedelta

from app.forecasting.forecaster import DailyCount, forecast


def _counts(need_type: str, values: list[int], start: date) -> list[DailyCount]:
    return [DailyCount(day=start + timedelta(days=i), needType=need_type, count=v) for i, v in enumerate(values)]


def test_rising_trend_is_detected_and_projected_upward():
    start = date(2026, 1, 1)
    rows = _counts("FOOD", [2, 4, 6, 8, 10], start)
    result = forecast(rows, horizon_days=3)
    assert len(result) == 1
    f = result[0]
    assert f.trend == "rising"
    assert f.forecast[0].projectedCount > rows[-1].count
    assert f.forecast[-1].day == start + timedelta(days=7)


def test_flat_trend_for_constant_counts():
    start = date(2026, 1, 1)
    rows = _counts("WATER", [5, 5, 5, 5], start)
    result = forecast(rows, horizon_days=2)
    assert result[0].trend == "flat"


def test_single_data_point_projects_flat_without_crashing():
    rows = [DailyCount(day=date(2026, 1, 1), needType="SHELTER", count=3)]
    result = forecast(rows, horizon_days=4)
    assert len(result[0].forecast) == 4
    assert all(p.projectedCount == 3 for p in result[0].forecast)


def test_multiple_need_types_forecast_independently():
    start = date(2026, 1, 1)
    rows = _counts("FOOD", [1, 2, 3], start) + _counts("MEDICINE", [10, 8, 6], start)
    result = forecast(rows, horizon_days=2)
    by_need = {r.needType: r for r in result}
    assert by_need["FOOD"].trend == "rising"
    assert by_need["MEDICINE"].trend == "falling"
