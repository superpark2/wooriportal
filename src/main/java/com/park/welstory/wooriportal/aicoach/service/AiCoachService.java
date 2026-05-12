package com.park.welstory.wooriportal.aicoach.service;

import com.park.welstory.wooriportal.aicoach.dto.AiCoachRequestDto;
import com.park.welstory.wooriportal.aicoach.dto.AiCoachResponseDto;
import com.park.welstory.wooriportal.aicoach.entity.AICoachAnswer;
import com.park.welstory.wooriportal.aicoach.entity.AICoachQuestion;
import com.park.welstory.wooriportal.aicoach.entity.AICoachSession;
import com.park.welstory.wooriportal.aicoach.repository.AnswerRepository;
import com.park.welstory.wooriportal.aicoach.repository.QuestionRepository;
import com.park.welstory.wooriportal.aicoach.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DB CRUD 전체 담당 서비스
 * Session / Question / Answer + Dashboard + 자동삭제 스케줄러
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCoachService {

    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    // ================================================================
    // SESSION
    // ================================================================

    @Transactional
    public Long createSession(AiCoachRequestDto.SessionCreateRequest req) {
        AICoachSession session = AICoachSession.builder()
                .job(req.getJob())
                .company(req.getCompany())
                .questionCount(req.getQuestionCount() != null ? req.getQuestionCount() : 5)
                .userId(req.getUserId())
                .build();
        return sessionRepository.save(session).getId();
    }

    @Transactional(readOnly = true)
    public List<AiCoachResponseDto.SessionResponse> getSessions(Long userId) {
        return sessionRepository.findByUserIdOrUserIdIsNull(userId).stream()
                .map(s -> AiCoachResponseDto.SessionResponse.from(s, avgScoreOfSession(s.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteSession(Long id) {
        sessionRepository.deleteById(id);   // cascade → questions, answers 자동 삭제
    }

    // ================================================================
    // QUESTION
    // ================================================================

    @Transactional
    public void saveQuestions(AiCoachRequestDto.QuestionSaveRequest req) {
        AICoachSession session = sessionRepository.findById(req.getSessionId())
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다: " + req.getSessionId()));

        List<String> list = req.getQuestions();
        for (int i = 0; i < list.size(); i++) {
            questionRepository.save(AICoachQuestion.builder()
                    .session(session)
                    .questionText(list.get(i))
                    .orderNum(i + 1)
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public List<AiCoachResponseDto.QuestionResponse> getQuestions(Long sessionId) {
        return questionRepository.findBySessionIdOrderByOrderNum(sessionId).stream()
                .map(AiCoachResponseDto.QuestionResponse::from)
                .collect(Collectors.toList());
    }

    // ================================================================
    // ANSWER
    // ================================================================

    @Transactional
    public Long saveAnswer(AiCoachRequestDto.AnswerSaveRequest req) {
        AICoachQuestion question = questionRepository.findById(req.getQuestionId())
                .orElseThrow(() -> new RuntimeException("질문을 찾을 수 없습니다: " + req.getQuestionId()));

        return answerRepository.save(AICoachAnswer.builder()
                .question(question)
                .answerText(req.getAnswerText())
                .feedbackGood(req.getFeedbackGood())
                .feedbackImprove(req.getFeedbackImprove())
                .score(req.getScore())
                .timeTaken(req.getTimeTaken()           != null ? req.getTimeTaken()         : 0)
                .starScore(req.getStarScore()           != null ? req.getStarScore()         : 0)
                .relevanceScore(req.getRelevanceScore() != null ? req.getRelevanceScore()   : 0)
                .detailScore(req.getDetailScore()       != null ? req.getDetailScore()       : 0)
                .build()).getId();
    }

    @Transactional(readOnly = true)
    public List<AiCoachResponseDto.AnswerResponse> getAnswers(Long questionId) {
        return answerRepository.findByQuestionIdOrderByCreatedAtDesc(questionId).stream()
                .map(AiCoachResponseDto.AnswerResponse::from)
                .collect(Collectors.toList());
    }

    // ================================================================
    // DASHBOARD
    // ================================================================

    @Transactional(readOnly = true)
    public AiCoachResponseDto.DashboardResponse getDashboard(Long userId) {
        long totalSessions = sessionRepository.countByUserId(userId);
        long monthSessions = sessionRepository.countByUserIdThisMonth(userId);
        int  avgScore      = answerRepository.findAvgScoreByUserId(userId)
                                .map(d -> (int) Math.round(d)).orElse(0);

        AICoachAnswer bestAICoachAnswer = answerRepository.findTopScoreByUserId(userId).orElse(null);
        int    bestScore   = bestAICoachAnswer != null ? bestAICoachAnswer.getScore() : 0;
        String bestLabel   = bestAICoachAnswer != null
                ? bestAICoachAnswer.getQuestion().getSession().getJob()
                  + " · " + bestAICoachAnswer.getQuestion().getSession().getCompany()
                : "-";

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M월 d일");

        List<AiCoachResponseDto.RecentSessionDto> recent = sessionRepository
                .findTop8ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(s -> AiCoachResponseDto.RecentSessionDto.builder()
                        .id(s.getId()).job(s.getJob()).company(s.getCompany())
                        .avgScore(avgScoreOfSession(s.getId()))
                        .date(s.getCreatedAt() != null ? s.getCreatedAt().format(fmt) : "")
                        .build())
                .collect(Collectors.toList());

        return AiCoachResponseDto.DashboardResponse.builder()
                .totalSessions(totalSessions).avgScore(avgScore)
                .bestScore(bestScore).bestLabel(bestLabel)
                .monthSessions(monthSessions).recentSessions(recent)
                .build();
    }

    // ================================================================
    // SCHEDULER - 24시간 만료 세션 자동 삭제 (매 1시간)
    // ================================================================

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deleteOldSessions() {
        List<AICoachSession> old = sessionRepository.findByCreatedAtBefore(LocalDateTime.now().minusDays(1));
        if (old.isEmpty()) { log.info("🧹 삭제할 만료 세션 없음"); return; }
        sessionRepository.deleteAll(old);
        log.info("🧹 만료 세션 {}개 자동 삭제 완료", old.size());
    }

    // ── 내부 헬퍼 ───────────────────────────────────────────────────
    private int avgScoreOfSession(Long sessionId) {
        return answerRepository.findAvgScoreBySessionId(sessionId)
                .map(d -> (int) Math.round(d)).orElse(0);
    }
}
