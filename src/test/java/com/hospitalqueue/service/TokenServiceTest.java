package com.hospitalqueue.service;

import com.hospitalqueue.entity.Doctor;
import com.hospitalqueue.entity.Patient;
import com.hospitalqueue.entity.Priority;
import com.hospitalqueue.entity.Token;
import com.hospitalqueue.entity.TokenStatus;
import com.hospitalqueue.repository.DoctorRepository;
import com.hospitalqueue.repository.PatientRepository;
import com.hospitalqueue.repository.TokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private PatientRepository patientRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private TokenRepository tokenRepository;
    @Mock
    private QueueBroadcastService queueBroadcastService;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(patientRepository, doctorRepository, tokenRepository, queueBroadcastService);
    }

    @Test
    void registerAssignsNextTokenNumberAndBroadcasts() {
        Long doctorId = 1L;
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(mock(Doctor.class)));
        when(patientRepository.save(any(Patient.class))).thenReturn(mock(Patient.class));
        when(tokenRepository.findMaxTokenNumberForDoctorBetween(eq(doctorId), any(), any())).thenReturn(3);
        when(tokenRepository.save(any(Token.class))).thenAnswer(inv -> inv.getArgument(0));

        Token token = tokenService.register("Raj Malhotra", "9999999999", doctorId, Priority.NORMAL);

        assertThat(token.getTokenNumber()).isEqualTo(4);
        assertThat(token.getStatus()).isEqualTo(TokenStatus.WAITING);
        verify(queueBroadcastService).broadcastQueueChange(eq(doctorId), any());
    }

    @Test
    void registerThrowsWhenDoctorNotFound() {
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> tokenService.register("Raj Malhotra", "9999999999", 99L, Priority.NORMAL));
    }

    @Test
    void callNextThrowsWhenDoctorAlreadyHasInProgressToken() {
        Long doctorId = 1L;
        when(tokenRepository.findInProgressTokenForDoctor(doctorId)).thenReturn(Optional.of(mock(Token.class)));

        assertThrows(IllegalStateException.class, () -> tokenService.callNext(doctorId));
    }

    @Test
    void callNextThrowsWhenNoWaitingTokens() {
        Long doctorId = 1L;
        when(tokenRepository.findInProgressTokenForDoctor(doctorId)).thenReturn(Optional.empty());
        when(tokenRepository.findWaitingQueueForDoctor(doctorId)).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> tokenService.callNext(doctorId));
    }

    @Test
    void callNextMovesFirstWaitingTokenToInProgress() {
        Long doctorId = 1L;
        Token token = new Token(1, mock(Patient.class), mock(Doctor.class), Priority.NORMAL);
        when(tokenRepository.findInProgressTokenForDoctor(doctorId)).thenReturn(Optional.empty());
        when(tokenRepository.findWaitingQueueForDoctor(doctorId)).thenReturn(List.of(token));
        when(tokenRepository.save(any(Token.class))).thenAnswer(inv -> inv.getArgument(0));

        Token result = tokenService.callNext(doctorId);

        assertThat(result.getStatus()).isEqualTo(TokenStatus.IN_PROGRESS);
        assertThat(result.getCalledAt()).isNotNull();
        verify(queueBroadcastService).broadcastQueueChange(eq(doctorId), any());
    }

    @Test
    void completeSetsCompletedStatusAndCompletedAt() {
        Doctor doctor = mock(Doctor.class);
        when(doctor.getId()).thenReturn(1L);
        Token token = new Token(1, mock(Patient.class), doctor, Priority.NORMAL);
        when(tokenRepository.findById(5L)).thenReturn(Optional.of(token));
        when(tokenRepository.save(any(Token.class))).thenAnswer(inv -> inv.getArgument(0));

        Token result = tokenService.complete(5L);

        assertThat(result.getStatus()).isEqualTo(TokenStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
        verify(queueBroadcastService).broadcastQueueChange(eq(1L), any());
    }

    @Test
    void cancelSetsCancelledStatus() {
        Doctor doctor = mock(Doctor.class);
        when(doctor.getId()).thenReturn(1L);
        Token token = new Token(1, mock(Patient.class), doctor, Priority.NORMAL);
        when(tokenRepository.findById(5L)).thenReturn(Optional.of(token));
        when(tokenRepository.save(any(Token.class))).thenAnswer(inv -> inv.getArgument(0));

        Token result = tokenService.cancel(5L);

        assertThat(result.getStatus()).isEqualTo(TokenStatus.CANCELLED);
        verify(queueBroadcastService).broadcastQueueChange(eq(1L), any());
    }

    @Test
    void escalateChangesPriorityWhenTokenIsWaiting() {
        Doctor doctor = mock(Doctor.class);
        when(doctor.getId()).thenReturn(1L);
        Token token = new Token(1, mock(Patient.class), doctor, Priority.NORMAL);
        when(tokenRepository.findById(5L)).thenReturn(Optional.of(token));
        when(tokenRepository.save(any(Token.class))).thenAnswer(inv -> inv.getArgument(0));

        Token result = tokenService.escalate(5L, Priority.EMERGENCY);

        assertThat(result.getPriority()).isEqualTo(Priority.EMERGENCY);
        verify(queueBroadcastService).broadcastQueueChange(eq(1L), any());
    }

    @Test
    void escalateThrowsWhenTokenIsNotWaiting() {
        Token token = new Token(1, mock(Patient.class), mock(Doctor.class), Priority.NORMAL);
        token.setStatus(TokenStatus.COMPLETED);
        when(tokenRepository.findById(5L)).thenReturn(Optional.of(token));

        assertThrows(IllegalStateException.class, () -> tokenService.escalate(5L, Priority.EMERGENCY));
    }
}
