package com.mrpark.dev.wooriportal.aicoach.controller;

import com.mrpark.dev.wooriportal.aicoach.dto.AiCoachRequestDto;
import com.mrpark.dev.wooriportal.aicoach.dto.AiCoachResponseDto;
import com.mrpark.dev.wooriportal.aicoach.service.AiCoachAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI 기능 전체 REST 컨트롤러
 * 면접: 질문생성 / 피드백
 * 자소서: 맞춤법검사 / 피드백 / 이미지텍스트추출
 */
@RestController
@RequiredArgsConstructor
public class AiCoachAiController {

    private final AiCoachAiService aiService;

    // ================================================================
    // 면접 AI  /aicoach/api/...
    // ================================================================

    @PostMapping("/aicoach/api/generate-questions")
    public ResponseEntity<AiCoachResponseDto.ApiResponse<Map<String, Object>>> generateQuestions(
            @RequestBody AiCoachRequestDto.GenerateQuestionsRequest req) {
        return ResponseEntity.ok(
            AiCoachResponseDto.ApiResponse.ok(Map.of("questions", aiService.generateQuestions(req)))
        );
    }

    @PostMapping("/aicoach/api/generate-feedback")
    public ResponseEntity<AiCoachResponseDto.ApiResponse<Map<String, Object>>> generateFeedback(
            @RequestBody AiCoachRequestDto.GenerateFeedbackRequest req) {
        return ResponseEntity.ok(
            AiCoachResponseDto.ApiResponse.ok(Map.of("feedback", aiService.generateFeedback(req)))
        );
    }

    // ================================================================
    // 자소서 AI  /aicoach/api/resume/...
    // ================================================================

    @PostMapping("/aicoach/api/resume/spell-check")
    public ResponseEntity<AiCoachResponseDto.ApiResponse<Map<String, Object>>> spellCheck(
            @RequestBody AiCoachRequestDto.SpellCheckRequest req) {
        return ResponseEntity.ok(AiCoachResponseDto.ApiResponse.ok(aiService.spellCheck(req)));
    }

    @PostMapping("/aicoach/api/resume/feedback")
    public ResponseEntity<AiCoachResponseDto.ApiResponse<Map<String, Object>>> resumeFeedback(
            @RequestBody AiCoachRequestDto.ResumeFeedbackRequest req) {
        return ResponseEntity.ok(AiCoachResponseDto.ApiResponse.ok(aiService.resumeFeedback(req)));
    }

    @PostMapping("/aicoach/api/resume/extract-image")
    public ResponseEntity<AiCoachResponseDto.ApiResponse<Map<String, String>>> extractImage(
            @RequestBody AiCoachRequestDto.ExtractImageRequest req) {
        return ResponseEntity.ok(
            AiCoachResponseDto.ApiResponse.ok(Map.of("text", aiService.extractTextFromImage(req)))
        );
    }
}
