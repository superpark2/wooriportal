package com.park.welstory.wooriportal.ai.excel;

import com.park.welstory.wooriportal.ai.AiHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
@RequiredArgsConstructor
public class ExcelController {

    private final ExcelAiService excelAiService;
    private final AiHandler aiHandler;  // 세션·heartbeat·취소 전파

    /** 엑셀 AI 처리 전용 페이지 */
    @GetMapping("/excel")
    public String excelPage() { return "excel"; }

    /**
     * 엑셀 업로드 + AI 처리 → SSE 스트리밍.
     *
     * <p>세션 생성 → heartbeat 시작 → 비동기 처리.
     * 브라우저 종료 / 타임아웃 / 중단 버튼 모두 ctx.cancel() 로 통일.
     * cancel() 이 호출되면 AiHandler 가 FastAPI + Ollama 취소까지 전파한다.</p>
     */
    @PostMapping(value = "/excel/process", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter processExcel(
            @RequestParam("file")   MultipartFile file,
            @RequestParam("prompt") String prompt,
            HttpServletResponse response) {

        response.setHeader("Cache-Control",     "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        response.setCharacterEncoding("UTF-8");

        SseEmitter emitter = new SseEmitter(600_000L); // 10분

        // ── 세션 생성 + heartbeat 시작 ──────────────────────────────
        AiHandler.SessionContext ctx = aiHandler.createSession(
                emitter, null, AiHandler.SessionType.EXCEL);

        // ── SSE 콜백 (브라우저 종료 / 타임아웃 / 에러 → cancel) ────────
        emitter.onTimeout(ctx::cancel);
        emitter.onError(e -> ctx.cancel());
        emitter.onCompletion(ctx::complete);

        // ── 비동기 처리 ─────────────────────────────────────────────
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                excelAiService.processExcel(file, prompt, ctx);
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"message\":\"서버 오류: " + e.getMessage() + "\"}"));
                } catch (Exception ignored) {}
                ctx.cancel();
            } finally {
                executor.shutdown();
            }
        });

        return emitter;
    }

    /**
     * 처리된 엑셀 파일 다운로드.
     * GET /excel/download?fileName=result_xxx.xlsx
     */
    @GetMapping("/excel/download")
    public ResponseEntity<byte[]> downloadExcel(@RequestParam("fileName") String fileName) {
        try {
            if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
                return ResponseEntity.badRequest().build();
            }
            byte[] data = excelAiService.getResultFile(fileName);
            String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + encodedName)
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        } catch (Exception e) {
            System.err.println("[ExcelController] 다운로드 오류: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
