package com.hospitalqueue.repository;

import com.hospitalqueue.entity.Token;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {

    /**
     * Highest token_number already issued to this doctor within [start, end)
     * — used to assign the next sequential number for that doctor's day.
     * Returns 0 if none issued yet.
     */
    @Query("SELECT COALESCE(MAX(t.tokenNumber), 0) FROM Token t "
            + "WHERE t.doctor.id = :doctorId AND t.createdAt >= :start AND t.createdAt < :end")
    int findMaxTokenNumberForDoctorBetween(@Param("doctorId") Long doctorId,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    /**
     * The single definition of queue order (CLAUDE.md architecture rule):
     * EMERGENCY, then PRIORITY, then NORMAL; FIFO by created_at within each
     * tier. Every screen/service that needs queue order (dashboard, patient
     * position, wait estimate) must call this method rather than
     * re-implementing the ordering.
     */
    @Query("SELECT t FROM Token t "
            + "WHERE t.doctor.id = :doctorId AND t.status = com.hospitalqueue.entity.TokenStatus.WAITING "
            + "ORDER BY CASE t.priority "
            + "    WHEN com.hospitalqueue.entity.Priority.EMERGENCY THEN 0 "
            + "    WHEN com.hospitalqueue.entity.Priority.PRIORITY THEN 1 "
            + "    ELSE 2 END, "
            + "t.createdAt ASC")
    List<Token> findWaitingQueueForDoctor(@Param("doctorId") Long doctorId);

    /** At most one token can be IN_PROGRESS per doctor at a time. */
    @Query("SELECT t FROM Token t "
            + "WHERE t.doctor.id = :doctorId AND t.status = com.hospitalqueue.entity.TokenStatus.IN_PROGRESS")
    Optional<Token> findInProgressTokenForDoctor(@Param("doctorId") Long doctorId);

    /** Most recently completed tokens for a doctor, used by WaitTimeEstimator's rolling average. */
    @Query("SELECT t FROM Token t "
            + "WHERE t.doctor.id = :doctorId AND t.status = com.hospitalqueue.entity.TokenStatus.COMPLETED "
            + "ORDER BY t.completedAt DESC")
    List<Token> findRecentCompletedTokensForDoctor(@Param("doctorId") Long doctorId, Pageable pageable);

    /** Completed tokens since a given instant, for historical analytics. */
    @Query("SELECT t FROM Token t "
            + "WHERE t.status = com.hospitalqueue.entity.TokenStatus.COMPLETED AND t.completedAt >= :since "
            + "ORDER BY t.completedAt")
    List<Token> findCompletedSince(@Param("since") LocalDateTime since);
}
