package com.hospitalqueue.dto;

import java.time.LocalDate;

/**
 * One day's aggregate over completed tokens. Built in plain Java from
 * {@code Token} rows — no separate analytics table, no DB-specific date
 * functions (see CLAUDE.md).
 */
public class DailyStats {

    private final LocalDate date;
    private final long totalCompleted;
    private final double avgConsultationMinutes;
    private final double avgWaitMinutes;

    public DailyStats(LocalDate date, long totalCompleted, double avgConsultationMinutes, double avgWaitMinutes) {
        this.date = date;
        this.totalCompleted = totalCompleted;
        this.avgConsultationMinutes = avgConsultationMinutes;
        this.avgWaitMinutes = avgWaitMinutes;
    }

    public LocalDate getDate() {
        return date;
    }

    public long getTotalCompleted() {
        return totalCompleted;
    }

    public double getAvgConsultationMinutes() {
        return avgConsultationMinutes;
    }

    public double getAvgWaitMinutes() {
        return avgWaitMinutes;
    }
}
