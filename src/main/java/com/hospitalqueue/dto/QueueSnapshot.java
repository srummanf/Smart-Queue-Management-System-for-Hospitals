package com.hospitalqueue.dto;

import java.util.List;

/**
 * Full re-sorted queue for one doctor, broadcast on every queue change.
 */
public class QueueSnapshot {

    private final Long doctorId;
    private final String doctorName;
    private final TokenView currentlyServing;
    private final List<TokenView> waitingTokens;

    public QueueSnapshot(Long doctorId, String doctorName, TokenView currentlyServing, List<TokenView> waitingTokens) {
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.currentlyServing = currentlyServing;
        this.waitingTokens = waitingTokens;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public TokenView getCurrentlyServing() {
        return currentlyServing;
    }

    public List<TokenView> getWaitingTokens() {
        return waitingTokens;
    }
}
