package com.reliefgrid.request;

import java.time.LocalDate;

/** Projection for the daily-counts-by-need-type query backing demand forecasting (Phase 3). */
public interface DailyNeedCount {
    LocalDate getDay();
    NeedType getNeedType();
    Long getCount();
}
