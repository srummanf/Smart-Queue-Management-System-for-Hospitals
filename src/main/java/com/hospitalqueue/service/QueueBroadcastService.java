package com.hospitalqueue.service;

import com.hospitalqueue.dto.PatientStatusView;
import com.hospitalqueue.dto.QueueSnapshot;
import com.hospitalqueue.dto.TokenView;
import com.hospitalqueue.entity.Doctor;
import com.hospitalqueue.entity.Token;
import com.hospitalqueue.entity.TokenStatus;
import com.hospitalqueue.repository.DoctorRepository;
import com.hospitalqueue.repository.TokenRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Single place that pushes STOMP messages after a queue change. TokenService
 * calls {@link #broadcastQueueChange} after every state-changing operation
 * (register, call-next, complete, cancel, escalate) — see CLAUDE.md
 * architecture rules. It pushes both the doctor's dashboard topic and the
 * patient-status topic for every token still WAITING (their position/ETA may
 * have shifted) plus the token that was just mutated.
 */
@Service
public class QueueBroadcastService {

    private static final int TURN_NEAR_POSITION = 2;
    private static final int TURN_NEAR_MINUTES = 10;

    private final SimpMessagingTemplate messagingTemplate;
    private final TokenRepository tokenRepository;
    private final DoctorRepository doctorRepository;
    private final WaitTimeEstimator waitTimeEstimator;

    public QueueBroadcastService(SimpMessagingTemplate messagingTemplate,
                                  TokenRepository tokenRepository,
                                  DoctorRepository doctorRepository,
                                  WaitTimeEstimator waitTimeEstimator) {
        this.messagingTemplate = messagingTemplate;
        this.tokenRepository = tokenRepository;
        this.doctorRepository = doctorRepository;
        this.waitTimeEstimator = waitTimeEstimator;
    }

    public void broadcastQueueChange(Long doctorId, Long affectedTokenId) {
        broadcastDoctorQueue(doctorId);

        List<Token> waiting = tokenRepository.findWaitingQueueForDoctor(doctorId);
        for (Token token : waiting) {
            broadcastPatientStatus(token.getId());
        }
        boolean affectedTokenAlreadyCovered = waiting.stream().anyMatch(t -> t.getId().equals(affectedTokenId));
        if (affectedTokenId != null && !affectedTokenAlreadyCovered) {
            broadcastPatientStatus(affectedTokenId);
        }
    }

    private void broadcastDoctorQueue(Long doctorId) {
        messagingTemplate.convertAndSend("/topic/doctor/" + doctorId + "/queue", buildSnapshot(doctorId));
    }

    private void broadcastPatientStatus(Long tokenId) {
        messagingTemplate.convertAndSend("/topic/patient/" + tokenId, buildPatientStatusView(tokenId));
    }

    /**
     * Builds the same queue snapshot used for broadcasting, so the
     * dashboard's initial page render and the live STOMP push are never two
     * different code paths.
     */
    public QueueSnapshot buildSnapshot(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + doctorId));

        TokenView currentlyServing = tokenRepository.findInProgressTokenForDoctor(doctorId)
                .map(token -> new TokenView(token, null, null))
                .orElse(null);

        List<Token> waiting = tokenRepository.findWaitingQueueForDoctor(doctorId);
        int avgConsultationMinutes = waitTimeEstimator.averageConsultationMinutes(doctorId);
        List<TokenView> views = new ArrayList<>(waiting.size());
        for (int i = 0; i < waiting.size(); i++) {
            int position = i + 1;
            int estimatedWaitMinutes = (position - 1) * avgConsultationMinutes;
            views.add(new TokenView(waiting.get(i), position, estimatedWaitMinutes));
        }

        return new QueueSnapshot(doctor.getId(), doctor.getName(), currentlyServing, views);
    }

    /**
     * Builds the same per-patient view used for broadcasting, reusing
     * {@link #buildSnapshot} for the WAITING case so position/ETA are never
     * computed a second way.
     */
    public PatientStatusView buildPatientStatusView(Long tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token not found: " + tokenId));

        if (token.getStatus() != TokenStatus.WAITING) {
            return new PatientStatusView(token, null, null, false);
        }

        QueueSnapshot snapshot = buildSnapshot(token.getDoctor().getId());
        TokenView view = snapshot.getWaitingTokens().stream()
                .filter(t -> t.getTokenId().equals(tokenId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Waiting token missing from its own queue snapshot: " + tokenId));

        boolean turnNear = view.getPosition() <= TURN_NEAR_POSITION || view.getEstimatedWaitMinutes() <= TURN_NEAR_MINUTES;
        return new PatientStatusView(token, view.getPosition(), view.getEstimatedWaitMinutes(), turnNear);
    }
}
