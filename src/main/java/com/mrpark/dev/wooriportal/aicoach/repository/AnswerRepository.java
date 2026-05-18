package com.mrpark.dev.wooriportal.aicoach.repository;

import com.mrpark.dev.wooriportal.aicoach.entity.AICoachAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnswerRepository extends JpaRepository<AICoachAnswer, Long> {

    List<AICoachAnswer> findByQuestionIdOrderByCreatedAtDesc(Long questionId);

    @Query("""
        SELECT AVG(a.score) FROM AICoachAnswer a
        JOIN a.question q
        JOIN q.session s
        WHERE (:userId IS NULL OR s.userId = :userId)
          AND a.score > 0
    """)
    Optional<Double> findAvgScoreByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT a FROM AICoachAnswer a
        JOIN a.question q
        JOIN q.session s
        WHERE (:userId IS NULL OR s.userId = :userId)
        ORDER BY a.score DESC
        LIMIT 1
    """)
    Optional<AICoachAnswer> findTopScoreByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT ROUND(AVG(a.score), 0) FROM AICoachAnswer a
        WHERE a.question.session.id = :sessionId
          AND a.score > 0
    """)
    Optional<Double> findAvgScoreBySessionId(@Param("sessionId") Long sessionId);
}
