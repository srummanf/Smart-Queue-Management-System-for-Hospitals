package com.hospitalqueue.service;

import com.hospitalqueue.entity.Token;
import com.hospitalqueue.repository.TokenRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Plain statistical wait-time estimate: rolling average consultation time
 * (over a doctor's recent completed tokens) times queue position. No ML —
 * see PRD/PLAN for why.
 */
@Service
public class WaitTimeEstimator {

    static final int HISTORY_SIZE = 10;
    static final int DEFAULT_AVERAGE_MINUTES = 15;

    private final TokenRepository tokenRepository;

    public WaitTimeEstimator(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public int averageConsultationMinutes(Long doctorId) {
        List<Token> recent = tokenRepository.findRecentCompletedTokensForDoctor(doctorId, PageRequest.of(0, HISTORY_SIZE));
        if (recent.isEmpty()) {
            return DEFAULT_AVERAGE_MINUTES;
        }

        long totalMinutes = 0;
        for (Token token : recent) {
            totalMinutes += Duration.between(token.getCalledAt(), token.getCompletedAt()).toMinutes();
        }
        return (int) Math.max(1, totalMinutes / recent.size());
    }

    /**
     * @param positionAheadInQueue number of patients ahead (0 for the next
     *                             patient to be called)
     */
    public int estimateWaitMinutes(Long doctorId, int positionAheadInQueue) {
        return positionAheadInQueue * averageConsultationMinutes(doctorId);
    }
}
