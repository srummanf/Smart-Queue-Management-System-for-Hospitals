package com.hospitalqueue.dto;

import com.hospitalqueue.entity.Priority;
import com.hospitalqueue.entity.Token;
import com.hospitalqueue.entity.TokenStatus;

/**
 * What the unauthenticated /patients/{tokenId} page shows and receives over
 * its STOMP topic. {@code position}/{@code estimatedWaitMinutes} are null
 * once the token is no longer WAITING.
 */
public class PatientStatusView {

    private final Long tokenId;
    private final Integer tokenNumber;
    private final String doctorName;
    private final Priority priority;
    private final TokenStatus status;
    private final Integer position;
    private final Integer estimatedWaitMinutes;
    private final boolean turnNear;

    public PatientStatusView(Token token, Integer position, Integer estimatedWaitMinutes, boolean turnNear) {
        this.tokenId = token.getId();
        this.tokenNumber = token.getTokenNumber();
        this.doctorName = token.getDoctor().getName();
        this.priority = token.getPriority();
        this.status = token.getStatus();
        this.position = position;
        this.estimatedWaitMinutes = estimatedWaitMinutes;
        this.turnNear = turnNear;
    }

    public Long getTokenId() {
        return tokenId;
    }

    public Integer getTokenNumber() {
        return tokenNumber;
    }

    public String getDoctorName() {
        return doctorName;
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

    public boolean isTurnNear() {
        return turnNear;
    }
}
