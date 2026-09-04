package com.hospitalqueue.service;

import com.hospitalqueue.dto.DailyStats;
import com.hospitalqueue.dto.DoctorStats;
import com.hospitalqueue.entity.Token;
import com.hospitalqueue.repository.TokenRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Historical stats read straight from completed {@code Token} rows — no
 * separate analytics/history table (see CLAUDE.md). Aggregation is done in
 * plain Java rather than DB-specific date-truncation SQL, to stay portable.
 */
@Service
public class AnalyticsService {

    static final int LOOKBACK_DAYS = 14;

    private final TokenRepository tokenRepository;

    public AnalyticsService(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public List<DailyStats> dailyStats() {
        List<Token> completed = completedTokensInLookbackWindow();

        Map<LocalDate, List<Token>> byDate = completed.stream()
                .collect(Collectors.groupingBy(t -> t.getCompletedAt().toLocalDate()));

        List<DailyStats> stats = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Token>> entry : byDate.entrySet()) {
            List<Token> tokens = entry.getValue();
            stats.add(new DailyStats(
                    entry.getKey(),
                    tokens.size(),
                    averageMinutes(tokens, Token::getCalledAt, Token::getCompletedAt),
                    averageMinutes(tokens, Token::getCreatedAt, Token::getCalledAt)));
        }
        stats.sort(Comparator.comparing(DailyStats::getDate));
        return stats;
    }

    public List<DoctorStats> doctorStats() {
        List<Token> completed = completedTokensInLookbackWindow();

        Map<Long, List<Token>> byDoctorId = completed.stream()
                .collect(Collectors.groupingBy(t -> t.getDoctor().getId()));

        List<DoctorStats> stats = new ArrayList<>();
        for (Map.Entry<Long, List<Token>> entry : byDoctorId.entrySet()) {
            List<Token> tokens = entry.getValue();
            String doctorName = tokens.get(0).getDoctor().getName();
            stats.add(new DoctorStats(
                    entry.getKey(),
                    doctorName,
                    tokens.size(),
                    averageMinutes(tokens, Token::getCalledAt, Token::getCompletedAt)));
        }
        stats.sort(Comparator.comparing(DoctorStats::getDoctorName));
        return stats;
    }

    private List<Token> completedTokensInLookbackWindow() {
        LocalDateTime since = LocalDate.now().minusDays(LOOKBACK_DAYS - 1L).atStartOfDay();
        return tokenRepository.findCompletedSince(since);
    }

    private double averageMinutes(List<Token> tokens,
                                   java.util.function.Function<Token, LocalDateTime> start,
                                   java.util.function.Function<Token, LocalDateTime> end) {
        return tokens.stream()
                .mapToLong(t -> Duration.between(start.apply(t), end.apply(t)).toMinutes())
                .average()
                .orElse(0);
    }
}
