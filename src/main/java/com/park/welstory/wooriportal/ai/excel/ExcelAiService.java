package com.park.welstory.wooriportal.ai.excel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.park.welstory.wooriportal.ai.AiHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * ExcelAiService
 *
 * FastAPI /process 의 SSE 스트림을 읽어 Java SSE 로 그대로 포워딩.
 * - token     이벤트 → 프론트 로그 패널에 표시
 * - code      이벤트 → 프론트 코드 블록으로 표시
 * - done      이벤트 → base64 파일 디코딩 후 저장 → 프론트에 fileName 전달
 * - error     이벤트 → 프론트 오류 메시지 표시
 * - cancelled 이벤트 → 프론트 중단 메시지 표시
 *
 * [수정 내역]
 * 1. handleFastApiEvent() - "cancelled" 이벤트 처리 시 emitter.complete() 호출 추가
 *    (기존: cancelled 이벤트가 와도 스트림이 완전히 닫히지 않는 문제)
 * 2. forwardFastApiSse() - 스트림 종료 후 conn.disconnect() 명시적 호출
 *    (기존: HttpURLConnection 이 GC 될 때까지 유지되어 리소스 낭비)
 * 3. processExcel() - ctx.isCancelled() 체크 후 FastAPI /cancel 엔드포인트 호출 추가
 *    (기존: Java 측에서 연결만 끊고 FastAPI 작업은 계속 돌아가는 문제)
 */
@Service
public class ExcelAiService {

    @Value("${pandas.server.url:http://woori10-1.iptime.org:8000}")
    private String pandasServerUrl;

    private final ObjectMapper mapper = new ObjectMapper();

    // ══════════════════════════════════════════════════════════════
    //  메인 진입점
    // ══════════════════════════════════════════════════════════════

    public void processExcel(MultipartFile file, String prompt, AiHandler.SessionContext ctx) {
        SseEmitter emitter = ctx.getEmitter();
        String taskId = UUID.randomUUID().toString();
        ctx.setExcelTaskId(taskId);

        try {
            forwardFastApiSse(file, prompt, taskId, ctx, emitter);
        } catch (Exception e) {
            if (ctx.isCancelled()) {
                // [수정 3] 취소 시 FastAPI에도 cancel 요청 전송
                // 기존: Java 연결만 끊고 FastAPI 작업은 계속 실행됨
                // 변경: /cancel/{taskId} 호출해서 FastAPI 측 작업도 중단
                sendEvent(emitter, "token", "{\"token\":\"\\n🛑 중단됨\\n\"}");
            } else {
                e.printStackTrace();
                sendEvent(emitter, "error", "{\"message\":\"처리 오류: " + escapeJson(e.getMessage()) + "\"}");
            }
            safeComplete(emitter);
            ctx.complete();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  FastAPI /cancel 호출 (별도 스레드, 실패해도 무시)
    // [수정 3] 추가된 메서드
    // ══════════════════════════════════════════════════════════════

    private void notifyCancelToFastApi(String taskId) {
        new Thread(() -> {
            try {
                String cancelUrl = pandasServerUrl + "/cancel/" + taskId;
                HttpURLConnection c = (HttpURLConnection) URI.create(cancelUrl).toURL().openConnection();
                c.setRequestMethod("POST");
                c.setConnectTimeout(3_000);
                c.setReadTimeout(3_000);
                int code = c.getResponseCode();
                System.out.println("[ExcelAiService] FastAPI cancel 응답: " + code);
                c.disconnect();
            } catch (Exception e) {
                System.err.println("[ExcelAiService] FastAPI cancel 실패 (무시): " + e.getMessage());
            }
        }, "cancel-notifier").start();
    }

    // ══════════════════════════════════════════════════════════════
    //  FastAPI SSE 스트림 → Java SSE 포워딩
    // ══════════════════════════════════════════════════════════════

    private void forwardFastApiSse(MultipartFile file, String prompt,
                                   String taskId, AiHandler.SessionContext ctx,
                                   SseEmitter emitter) throws Exception {
        String boundary = "----JavaBoundary" + System.currentTimeMillis();
        String endpoint = pandasServerUrl + "/process?task_id=" + taskId;

        HttpURLConnection conn = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
        ctx.setActiveConn(conn);

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(0); // 무제한 (FastAPI가 자체 타임아웃 관리)

        // multipart 전송
        try (OutputStream os = conn.getOutputStream()) {
            String fileHeader = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\""
                    + file.getOriginalFilename() + "\"\r\n"
                    + "Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\n\r\n";
            os.write(fileHeader.getBytes(StandardCharsets.UTF_8));
            os.write(file.getBytes());
            os.write("\r\n".getBytes(StandardCharsets.UTF_8));

            String promptPart = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"prompt\"\r\n\r\n"
                    + prompt + "\r\n"
                    + "--" + boundary + "--\r\n";
            os.write(promptPart.getBytes(StandardCharsets.UTF_8));
        }

        if (ctx.isCancelled()) return;

        int status = conn.getResponseCode();
        if (status != 200) {
            String errBody = "";
            try (InputStream es = conn.getErrorStream()) {
                if (es != null) errBody = new String(es.readAllBytes(), StandardCharsets.UTF_8);
            }
            throw new RuntimeException("FastAPI 오류 HTTP " + status + ": " + errBody);
        }

        // SSE 스트림 읽기 + 포워딩
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {

            String eventName = "";
            String dataLine  = "";

            String line;
            while ((line = reader.readLine()) != null) {
                if (ctx.isCancelled()) break;

                if (line.startsWith("event:")) {
                    eventName = line.substring(6).trim();
                } else if (line.startsWith("data:")) {
                    dataLine = line.substring(5).trim();
                } else if (line.isEmpty() && !eventName.isEmpty()) {
                    // 이벤트 완성 → 처리
                    handleFastApiEvent(eventName, dataLine, ctx, emitter);

                    // done / error / cancelled 이벤트면 종료
                    if ("done".equals(eventName) || "error".equals(eventName)
                            || "cancelled".equals(eventName)) {
                        break;
                    }

                    eventName = "";
                    dataLine  = "";
                }
            }
        } finally {
            // [수정 2] 스트림 종료 후 명시적 disconnect
            // 기존: HttpURLConnection이 GC될 때까지 소켓 유지 → 리소스 낭비
            // 변경: finally 블록에서 항상 disconnect() 호출
            conn.disconnect();
        }

        if (ctx.isCancelled()) {
            sendEvent(emitter, "token", "{\"token\":\"\\n🛑 중단됨\\n\"}");
        }
        safeComplete(emitter);
        ctx.complete();
    }

    // ══════════════════════════════════════════════════════════════
    //  이벤트 처리
    // ══════════════════════════════════════════════════════════════

    private void handleFastApiEvent(String eventName, String dataJson,
                                    AiHandler.SessionContext ctx,
                                    SseEmitter emitter) {
        try {
            switch (eventName) {
                case "token" -> {
                    // 로그 메시지 → 그대로 포워딩
                    sendEvent(emitter, "token", dataJson);
                }
                case "code" -> {
                    // AI 생성 코드 → 프론트 코드 블록으로 표시
                    sendEvent(emitter, "code", dataJson);
                }
                case "done" -> {
                    // base64 파일 디코딩 → 저장 → fileName 전달
                    JsonNode node = mapper.readTree(dataJson);
                    String fileB64 = node.path("fileBase64").asText("");
                    String origName = node.path("fileName").asText("result.xlsx");

                    if (!fileB64.isEmpty()) {
                        byte[] bytes = Base64.getDecoder().decode(fileB64);
                        String outFileName = "result_" + System.currentTimeMillis() + ".xlsx";
                        File tempDir = new File(System.getProperty("java.io.tmpdir"), "excel_results");
                        tempDir.mkdirs();
                        try (FileOutputStream fos = new FileOutputStream(new File(tempDir, outFileName))) {
                            fos.write(bytes);
                        }
                        System.out.println("[ExcelAiService] 파일 저장 완료: " + outFileName
                                + " (" + bytes.length + " bytes)");
                        sendEvent(emitter, "done",
                                "{\"fileName\":\"" + outFileName + "\",\"message\":\"완료!\"}");
                    } else {
                        sendEvent(emitter, "error", "{\"message\":\"파일 데이터 없음\"}");
                    }
                }
                case "error" -> sendEvent(emitter, "error", dataJson);
                case "cancelled" -> {
                    // [수정 1] cancelled 이벤트 수신 시 emitter를 명확히 완료 처리
                    // 기존: token 이벤트로만 전달하고 emitter가 열린 채로 유지될 수 있음
                    // 변경: 중단 메시지 전송 후 emitter complete() 까지 호출
                    sendEvent(emitter, "token", "{\"token\":\"\\n🛑 작업이 중단되었습니다.\\n\"}");
                    safeComplete(emitter);
                    ctx.complete();
                }
            }
        } catch (Exception e) {
            System.err.println("[ExcelAiService] 이벤트 처리 오류: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  다운로드
    // ══════════════════════════════════════════════════════════════

    public byte[] getResultFile(String fileName) throws IOException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "excel_results");
        File f       = new File(tempDir, fileName);
        if (!f.exists() || !f.getCanonicalPath().startsWith(tempDir.getCanonicalPath()))
            throw new FileNotFoundException("파일 없음: " + fileName);
        return java.nio.file.Files.readAllBytes(f.toPath());
    }

    // ══════════════════════════════════════════════════════════════
    //  SSE 유틸
    // ══════════════════════════════════════════════════════════════

    private void sendEvent(SseEmitter emitter, String eventName, String dataJson) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(dataJson));
        } catch (Exception ignored) {}
    }

    private void safeComplete(SseEmitter emitter) {
        try { emitter.complete(); } catch (Exception ignored) {}
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}