package com.hospitalqueue.dto;

public class DoctorStats {

    private final Long doctorId;
    private final String doctorName;
    private final long totalCompleted;
    private final double avgConsultationMinutes;

    public DoctorStats(Long doctorId, String doctorName, long totalCompleted, double avgConsultationMinutes) {
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.totalCompleted = totalCompleted;
        this.avgConsultationMinutes = avgConsultationMinutes;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public long getTotalCompleted() {
        return totalCompleted;
    }

    public double getAvgConsultationMinutes() {
        return avgConsultationMinutes;
    }
}
