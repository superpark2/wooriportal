package com.park.welstory.wooriportal.ai;

import com.park.welstory.wooriportal.ai.dto.AIDTO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
@RequiredArgsConstructor
public class AiController {

    private final AiHandler aiHandler;
    private final AiService aiService;

    /** SSE 비동기 처리용 공유 스레드풀 — 요청마다 생성/폐기하지 않음 */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /** 메인 채팅 페이지 */
    @GetMapping("/ai")
    public String index() {
        return "ai/index"; }

    /**
     * SSE 스트리밍 채팅.
     * 세션 생성 → heartbeat 시작 → 비동기 처리.
     * 브라우저 종료 / 타임아웃 / 에러 모두 ctx.cancel() 로 통일.
     */
    @PostMapping(value = "/ai/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter chat(
            @RequestBody AIDTO.ChatRequest request,
            HttpServletResponse response) {

        response.setHeader("Cache-Control",     "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        response.setCharacterEncoding("UTF-8");

        if (request.getSessionId() == null || request.getSessionId().isBlank()) {
            request.setSessionId("default");
        }

        SseEmitter emitter = new SseEmitter(300_000L); // 5분

        // ── 세션 생성 + heartbeat 시작 ──────────────────────────────
        AiHandler.SessionContext ctx = aiHandler.createSession(
                emitter, request.getSessionId(), AiHandler.SessionType.CHAT);

        // ── SSE 콜백 등록 (브라우저 종료 / 타임아웃 / 에러 → cancel) ──
        emitter.onTimeout(ctx::cancel);
        emitter.onError(e -> ctx.cancel());
        // onCompletion 은 정상 완료 시에도 호출되므로 complete() 사용
        emitter.onCompletion(ctx::complete);

        // ── 비동기 처리 ─────────────────────────────────────────────
        executor.submit(() -> {
            try {
                aiService.streamChat(request, ctx);
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"message\":\"서버 오류가 발생했습니다.\"}"));
                } catch (Exception ignored) {}
                ctx.cancel();
            }
        });

        return emitter;
    }

    /**
     * 중지 버튼 → 세션 취소.
     * heartbeat 중단 + Ollama/ComfyUI 취소 전파.
     */
    @PostMapping("/ai/chat/cancel")
    public ResponseEntity<Void> cancelChat(@RequestBody Map<String, String> body) {
        aiHandler.cancelSession(body.get("sessionId"));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/ai/chat/session/{sessionId}")
    public ResponseEntity<Void> clearSession(@PathVariable String sessionId) {
        aiService.clearSession(sessionId);
        return ResponseEntity.ok().build();
    }
}