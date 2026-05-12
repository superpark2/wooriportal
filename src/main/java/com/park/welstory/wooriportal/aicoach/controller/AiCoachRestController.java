package com.park.welstory.wooriportal.aicoach.controller;

import com.park.welstory.wooriportal.aicoach.dto.AiCoachRequestDto;
import com.park.welstory.wooriportal.aicoach.dto.AiCoachResponseDto;
import com.park.welstory.wooriportal.aicoach.service.AiCoachService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * DB CRUD 전체 REST 컨트롤러
 * Session / Question / Answer / Dashboard
 */
@Slf4j
@RestController
@RequestMapping("/aicoach/api")
@RequiredArgsConstructor
public class AiCoachRestController {

    private final AiCoachService aiCoachService;

    // ── DB 연결 테스트 ──────────────────────────────────────────────
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        return ResponseEntity.ok(Map.of("message", "✅ DB 연결 성공!"));
    }

    // ================================================================
    // SESSION
    // ================================================================

    @PostMapping("/sessions")
    public ResponseEntity<AiCoachResponseDto.ApiResponse<Map<String, Long>>> createSession(
            @RequestBody AiCoachRequestDto.SessionCreateRequest req) {
        Long id = aiCoachService.createSession(req);
        return ResponseEntity.ok(AiCoachResponseDto.ApiResponse.ok(Map.of("session_id", id)));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<AiCoachResponseDto.SessionResponse>> getSessions(
            @RequestParam(required = false) Long user_id) {
        return ResponseEntity.ok(aiCoachService.getSessions(user_id));
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<AiCoachResponseDto.ApiResponse<Void>> deleteSession(@PathVariable Long id) {
        aiCoachService.deleteSession(id);
        return ResponseEntity.ok(AiCoachResponseDto.ApiResponse.ok(null));
    }

    // ================================================================
    // QUESTION
    // ================================================================

    @PostMapping("/questions")
    public ResponseEntity<AiCoachResponseDto.ApiResponse<Void>> saveQuestions(
            @RequestBody AiCoachRequestDto.QuestionSaveRequest req) {
        aiCoachService.saveQuestions(req);
        return ResponseEntity.ok(AiCoachResponseDto.ApiResponse.ok(null));
    }

    @GetMapping("/questions/{sessionId}")
    public ResponseEntity<List<AiCoachResponseDto.QuestionResponse>> getQuestions(
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(aiCoachService.getQuestions(sessionId));
    }

    // ================================================================
    // ANSWER
    // ================================================================

    @PostMapping("/answers")
    public ResponseEntity<AiCoachResponseDto.ApiResponse<Map<String, Long>>> saveAnswer(
            @RequestBody AiCoachRequestDto.AnswerSaveRequest req) {
        Long id = aiCoachService.saveAnswer(req);
        return ResponseEntity.ok(AiCoachResponseDto.ApiResponse.ok(Map.of("answer_id", id)));
    }

    @GetMapping("/answers/{questionId}")
    public ResponseEntity<List<AiCoachResponseDto.AnswerResponse>> getAnswers(
            @PathVariable Long questionId) {
        return ResponseEntity.ok(aiCoachService.getAnswers(questionId));
    }

    // ================================================================
    // DASHBOARD
    // ================================================================

    @GetMapping("/dashboard")
    public ResponseEntity<AiCoachResponseDto.DashboardResponse> getDashboard(
            @RequestParam(required = false) Long user_id) {
        return ResponseEntity.ok(aiCoachService.getDashboard(user_id));
    }
}
