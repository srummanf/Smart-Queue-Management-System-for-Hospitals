package com.hospitalqueue.service;

import com.hospitalqueue.entity.Doctor;
import com.hospitalqueue.entity.Patient;
import com.hospitalqueue.entity.Priority;
import com.hospitalqueue.entity.Token;
import com.hospitalqueue.repository.TokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaitTimeEstimatorTest {

    @Mock
    private TokenRepository tokenRepository;

    private WaitTimeEstimator estimator;

    @BeforeEach
    void setUp() {
        estimator = new WaitTimeEstimator(tokenRepository);
    }

    @Test
    void fallsBackToDefaultWhenNoHistory() {
        when(tokenRepository.findRecentCompletedTokensForDoctor(eq(1L), any())).thenReturn(List.of());

        assertThat(estimator.averageConsultationMinutes(1L)).isEqualTo(WaitTimeEstimator.DEFAULT_AVERAGE_MINUTES);
    }

    @Test
    void computesAverageFromRecentCompletedTokens() {
        List<Token> recent = List.of(completedToken(10), completedToken(20), completedToken(30));
        when(tokenRepository.findRecentCompletedTokensForDoctor(eq(1L), any())).thenReturn(recent);

        assertThat(estimator.averageConsultationMinutes(1L)).isEqualTo(20);
    }

    @Test
    void estimateWaitMinutesMultipliesPositionByAverage() {
        when(tokenRepository.findRecentCompletedTokensForDoctor(eq(1L), any())).thenReturn(List.of(completedToken(10)));

        assertThat(estimator.estimateWaitMinutes(1L, 3)).isEqualTo(30);
    }

    private Token completedToken(int consultationMinutes) {
        Token token = new Token(1, mock(Patient.class), mock(Doctor.class), Priority.NORMAL);
        LocalDateTime calledAt = LocalDateTime.now();
        token.setCalledAt(calledAt);
        token.setCompletedAt(calledAt.plusMinutes(consultationMinutes));
        return token;
    }
}
