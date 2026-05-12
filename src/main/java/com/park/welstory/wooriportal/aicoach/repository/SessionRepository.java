package com.park.welstory.wooriportal.aicoach.repository;

import com.park.welstory.wooriportal.aicoach.entity.AICoachSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SessionRepository extends JpaRepository<AICoachSession, Long> {

    @Query("""
        SELECT s FROM AICoachSession s
        WHERE s.userId = :userId OR s.userId IS NULL
        ORDER BY s.createdAt DESC
    """)
    List<AICoachSession> findByUserIdOrUserIdIsNull(@Param("userId") Long userId);

    List<AICoachSession> findByCreatedAtBefore(LocalDateTime dateTime);

    @Query("""
        SELECT s FROM AICoachSession s
        WHERE (:userId IS NULL OR s.userId = :userId)
        ORDER BY s.createdAt DESC
        LIMIT 8
    """)
    List<AICoachSession> findTop8ByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("""
        SELECT COUNT(s) FROM AICoachSession s
        WHERE (:userId IS NULL OR s.userId = :userId)
    """)
    long countByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT COUNT(s) FROM AICoachSession s
        WHERE (:userId IS NULL OR s.userId = :userId)
          AND MONTH(s.createdAt) = MONTH(CURRENT_DATE)
          AND YEAR(s.createdAt) = YEAR(CURRENT_DATE)
    """)
    long countByUserIdThisMonth(@Param("userId") Long userId);
}
