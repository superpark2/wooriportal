package com.park.welstory.wooriportal.aicoach.repository;

import com.park.welstory.wooriportal.aicoach.entity.AICoachQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<AICoachQuestion, Long> {
    List<AICoachQuestion> findBySessionIdOrderByOrderNum(Long sessionId);
    void deleteBySessionId(Long sessionId);
}
