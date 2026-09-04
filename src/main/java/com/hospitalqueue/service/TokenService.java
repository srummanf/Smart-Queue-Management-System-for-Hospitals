package com.hospitalqueue.service;

import com.hospitalqueue.entity.Doctor;
import com.hospitalqueue.entity.Patient;
import com.hospitalqueue.entity.Priority;
import com.hospitalqueue.entity.Token;
import com.hospitalqueue.entity.TokenStatus;
import com.hospitalqueue.repository.DoctorRepository;
import com.hospitalqueue.repository.PatientRepository;
import com.hospitalqueue.repository.TokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TokenService {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final TokenRepository tokenRepository;
    private final QueueBroadcastService queueBroadcastService;

    public TokenService(PatientRepository patientRepository,
                         DoctorRepository doctorRepository,
                         TokenRepository tokenRepository,
                         QueueBroadcastService queueBroadcastService) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.tokenRepository = tokenRepository;
        this.queueBroadcastService = queueBroadcastService;
    }

    @Transactional
    public Token register(String patientFullName, String patientPhone, Long doctorId, Priority priority) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + doctorId));

        Patient patient = patientRepository.save(new Patient(patientFullName, patientPhone));

        int tokenNumber = nextTokenNumberFor(doctorId);
        Token token = tokenRepository.save(new Token(tokenNumber, patient, doctor, priority));

        queueBroadcastService.broadcastQueueChange(doctorId, token.getId());

        return token;
    }

    @Transactional
    public Token callNext(Long doctorId) {
        if (tokenRepository.findInProgressTokenForDoctor(doctorId).isPresent()) {
            throw new IllegalStateException("Doctor " + doctorId + " already has a patient in progress");
        }

        List<Token> waiting = tokenRepository.findWaitingQueueForDoctor(doctorId);
        if (waiting.isEmpty()) {
            throw new IllegalStateException("No waiting tokens for doctor " + doctorId);
        }

        Token token = waiting.get(0);
        token.setStatus(TokenStatus.IN_PROGRESS);
        token.setCalledAt(LocalDateTime.now());
        tokenRepository.save(token);

        queueBroadcastService.broadcastQueueChange(doctorId, token.getId());

        return token;
    }

    @Transactional
    public Token complete(Long tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token not found: " + tokenId));

        token.setStatus(TokenStatus.COMPLETED);
        token.setCompletedAt(LocalDateTime.now());
        tokenRepository.save(token);

        queueBroadcastService.broadcastQueueChange(token.getDoctor().getId(), token.getId());

        return token;
    }

    @Transactional
    public Token cancel(Long tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token not found: " + tokenId));

        token.setStatus(TokenStatus.CANCELLED);
        tokenRepository.save(token);

        queueBroadcastService.broadcastQueueChange(token.getDoctor().getId(), token.getId());

        return token;
    }

    @Transactional
    public Token escalate(Long tokenId, Priority newPriority) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token not found: " + tokenId));

        if (token.getStatus() != TokenStatus.WAITING) {
            throw new IllegalStateException("Only a WAITING token can be escalated: " + tokenId);
        }

        token.setPriority(newPriority);
        tokenRepository.save(token);

        queueBroadcastService.broadcastQueueChange(token.getDoctor().getId(), token.getId());

        return token;
    }

    private int nextTokenNumberFor(Long doctorId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime startOfNextDay = startOfDay.plusDays(1);
        int highestSoFar = tokenRepository.findMaxTokenNumberForDoctorBetween(doctorId, startOfDay, startOfNextDay);
        return highestSoFar + 1;
    }
}
