package com.hospitalqueue.dto;

import com.hospitalqueue.entity.Priority;
import com.hospitalqueue.entity.Token;
import com.hospitalqueue.entity.TokenStatus;

/**
 * What gets pushed over WebSocket / rendered — never the raw entity.
 * {@code position} and {@code estimatedWaitMinutes} are null for a token
 * that isn't currently WAITING (e.g. the one being served).
 */
public class TokenView {

    private final Long tokenId;
    private final Integer tokenNumber;
    private final String patientName;
    private final Priority priority;
    private final TokenStatus status;
    private final Integer position;
    private final Integer estimatedWaitMinutes;

    public TokenView(Token token, Integer position, Integer estimatedWaitMinutes) {
        this.tokenId = token.getId();
        this.tokenNumber = token.getTokenNumber();
        this.patientName = token.getPatient().getFullName();
        this.priority = token.getPriority();
        this.status = token.getStatus();
        this.position = position;
        this.estimatedWaitMinutes = estimatedWaitMinutes;
    }

    public Long getTokenId() {
        return tokenId;
    }

    public Integer getTokenNumber() {
        return tokenNumber;
    }

    public String getPatientName() {
        return patientName;
    }

    public Priority getPriority() {
        return priority;
    }

    public TokenStatus getStatus() {
        return status;
    }

    public Integer getPosition() {
        return position;
    }

    public Integer getEstimatedWaitMinutes() {
        return estimatedWaitMinutes;
    }
}
