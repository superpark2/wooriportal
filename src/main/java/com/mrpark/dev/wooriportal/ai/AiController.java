package com.mrpark.dev.wooriportal.ai;

import com.mrpark.dev.wooriportal.ai.dto.ChatRequestDTO;
import com.mrpark.dev.wooriportal.ai.mcp.tools.embedding.EmbeddingService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
@RequiredArgsConstructor
public class AiController {

    private final AiHandler        aiHandler;
    private final AiService        aiService;
    private final EmbeddingService embeddingService;

    /** SSE 비동기 처리용 공유 스레드풀 — 요청마다 생성/폐기하지 않음 */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /** 메인 채팅 페이지 */
    @GetMapping("/ai")
    public String index() {
        return "ai/index";
    }

    /** 임베딩 어드민 페이지 */
    @GetMapping("/ai/embedding")
    public String embeddingAdmin() {
        return "ai/embedding";
    }

    /**
     * SSE 스트리밍 채팅.
     * 세션 생성 → heartbeat 시작 → 비동기 처리.
     * 브라우저 종료 / 타임아웃 / 에러 모두 ctx.cancel() 로 통일.
     */
    @PostMapping(value = "/ai/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter chat(
            @RequestBody ChatRequestDTO request,
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

    // ══════════════════════════════════════════════════════════════
    //  임베딩 어드민 CRUD API
    // ══════════════════════════════════════════════════════════════

    /** 전체 문서 목록 */
    @GetMapping("/ai/docs")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getDocs() {
        return ResponseEntity.ok(embeddingService.getAllDocuments());
    }

    /** 문서 추가(id 없음) 또는 수정(id 있음) */
    @PostMapping("/ai/add-doc")
    @ResponseBody
    public ResponseEntity<Map<String, String>> addOrUpdateDoc(
            @RequestBody Map<String, Object> body) {
        try {
            Object idObj   = body.get("id");
            String category = (String) body.get("category");
            String title    = (String) body.get("title");
            String content  = (String) body.get("content");

            if (idObj != null) {
                int id = (idObj instanceof Integer) ? (Integer) idObj
                        : Integer.parseInt(idObj.toString());
                embeddingService.updateDocument(id, category, title, content);
            } else {
                embeddingService.addDocument(category, title, content);
            }
            return ResponseEntity.ok(Map.of("result", "ok"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /** 문서 삭제 */
    @PostMapping("/ai/delete-doc")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteDoc(
            @RequestBody Map<String, Object> body) {
        try {
            Object idObj = body.get("id");
            int id = (idObj instanceof Integer) ? (Integer) idObj
                    : Integer.parseInt(idObj.toString());
            embeddingService.deleteDocument(id);
            return ResponseEntity.ok(Map.of("result", "ok"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 파일에서 텍스트 추출 (현재 TXT 지원).
     * PDF/XLSX 지원은 pdfbox/poi 의존성 추가 후 확장 가능.
     */
    @PostMapping("/ai/parse-file")
    @ResponseBody
    public ResponseEntity<Map<String, String>> parseFile(
            @RequestParam("file") MultipartFile file) {
        try {
            String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();

            if (filename.endsWith(".txt")) {
                String content = new String(file.getBytes(), StandardCharsets.UTF_8);
                return ResponseEntity.ok(Map.of("content", content));
            }

            return ResponseEntity.badRequest()
                    .body(Map.of("error", "현재 .txt 파일만 지원합니다. PDF/XLSX는 의존성 추가 후 확장 가능합니다."));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "파일 처리 중 오류: " + e.getMessage()));
        }
    }

    /** 전체 문서 재임베딩 (임베딩 모델 변경 후 사용) */
    @PostMapping("/ai/re-embed")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> reEmbed() {
        try {
            int count = embeddingService.reembedAll();
            return ResponseEntity.ok(Map.of("result", "ok", "count", count));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}