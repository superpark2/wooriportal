package com.mrpark.dev.wooriportal.aicoach.controller;

import com.mrpark.dev.wooriportal.aicoach.dto.AiCoachResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 정적 리소스 없을 때 (/, /favicon.ico 등) → 조용하게 404만 반환
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AiCoachResponseDto.ApiResponse<Void>> handleBadRequest(IllegalArgumentException e) {
        log.warn("Bad request: {}", e.getMessage());
        return ResponseEntity.badRequest().body(AiCoachResponseDto.ApiResponse.fail(e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<AiCoachResponseDto.ApiResponse<Void>> handleRuntime(RuntimeException e) {
        log.error("Runtime error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AiCoachResponseDto.ApiResponse.fail(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AiCoachResponseDto.ApiResponse<Void>> handleGeneral(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AiCoachResponseDto.ApiResponse.fail("서버 오류가 발생했습니다."));
    }
}
