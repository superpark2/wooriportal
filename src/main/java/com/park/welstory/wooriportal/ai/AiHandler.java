package com.park.welstory.wooriportal.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ┌─────────────────────────────────────────────────────────────────┐
 * │                         AiHandler                              │
 * │  AI챗 / 엑셀 요청의 세션 생성 · heartbeat · 취소 전파를 담당한다.  │
 * │                                                                 │
 * │  [Heartbeat]                                                    │
 * │    세션 생성 직후부터 5초마다 SSE ping 이벤트 전송.               │
 * │    전송 실패(클라이언트 끊김) → cancel() 자동 호출.              │
 * │                                                                 │
 * │  [취소 전파 경로]                                                │
 * │    ① 중지 버튼  → AiController/ExcelController → cancelSession()│
 * │    ② 브라우저 종료 → heartbeat ping 실패 감지 → cancel()         │
 * │    ③ SSE timeout/error → emitter 콜백 → cancel()               │
 * │                                                                 │
 * │  [Ollama 언로드 정책]                                            │
 * │    취소 시 동일 타입(CHAT→CHAT / EXCEL→EXCEL) 세션이             │
 * │    활성 상태이면 언로드 스킵 → 모델 재로드 비용 절감.             │
 * │    타입이 바뀔 때(CHAT→EXCEL, EXCEL→CHAT)만 언로드.             │
 * │                                                                 │
 * │  [취소 시 전파 대상]                                             │
 * │    ① activeConn.disconnect()   : HTTP 소켓 즉시 끊기            │
 * │    ② Ollama keep_alive=0       : VRAM 즉시 해제 (조건부)        │
 * │    ③ ComfyUI /interrupt + /queue clear : 이미지 생성 중단        │
 * │    ④ FastAPI /cancel/{taskId}  : 엑셀 처리 중단                 │
 * └─────────────────────────────────────────────────────────────────┘
 */
@Component
public class AiHandler {

    @Value("${ollama.api.url:http://woori10-1.iptime.org:11434/api}")
    private String ollamaApiUrl;

    @Value("${ollama.model}")
    private String chatModel;

    @Value("${comfy.url:http://woori10-1.iptime.org:8188}")
    private String comfyUrl;

    @Value("${pandas.server.url:http://woori10-1.iptime.org:8000}")
    private String pandasServerUrl;

    @Value("${excel.model:gemma4:e4b}")
    private String excelModel;

    private final ObjectMapper mapper = new ObjectMapper();

    /** 활성 세션 맵 */
    private final Map<String, SessionContext> sessions = new ConcurrentHashMap<>();

    /**
     * 현재 Ollama VRAM에 올라와 있다고 간주되는 모델 타입.
     * null = 아무것도 안 올라옴 / 언로드 완료.
     * Chat 완료 후에도 CHAT 유지 → 다음 Chat 요청 시 재로드 비용 0.
     * ComfyUI / Excel 시작 직전에 CHAT이 올라와 있으면 그때 언로드.
     */
    private volatile SessionType lastLoadedType = null;

    /** heartbeat 전용 스케줄러 (데몬 스레드) */
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(4, r -> {
                Thread t = new Thread(r, "ai-heartbeat");
                t.setDaemon(true);
                return t;
            });

    private static final int HEARTBEAT_SEC = 5;

    // ════════════════════════════════════════════════════════════════
    //  세션 타입
    // ════════════════════════════════════════════════════════════════

    public enum SessionType { CHAT, EXCEL }

    // ════════════════════════════════════════════════════════════════
    //  SessionContext
    // ════════════════════════════════════════════════════════════════

    public class SessionContext {

        @Getter
        private final String      sessionId;
        @Getter
        private final SseEmitter  emitter;
        private final SessionType type;

        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final AtomicBoolean done      = new AtomicBoolean(false);

        /** Excel/ComfyUI HTTP 연결 등록 (취소 시 즉시 disconnect) */
        @Setter
        private volatile HttpURLConnection activeConn     = null;
        /** ComfyUI prompt_id 등록 */
        @Setter
        private volatile String            comfyPromptId  = null;
        /** FastAPI taskId 등록 */
        @Setter
        private volatile String            excelTaskId    = null;

        private ScheduledFuture<?> heartbeatTask;

        SessionContext(String sessionId, SseEmitter emitter, SessionType type) {
            this.sessionId = sessionId;
            this.emitter   = emitter;
            this.type      = type;
        }

        public boolean isCancelled() { return cancelled.get(); }

        /**
         * 정상 완료. heartbeat 중단 + 세션 맵 제거.
         * (취소 전파 없음)
         */
        public void complete() {
            if (done.compareAndSet(false, true)) {
                stopHeartbeat();
                sessions.remove(sessionId);
            }
        }

        /**
         * 취소. heartbeat 중단 → 취소 전파 → 세션 맵 제거.
         * 여러 경로에서 동시에 호출되어도 한 번만 실행된다.
         */
        public void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                done.set(true);
                stopHeartbeat();
                sessions.remove(sessionId);   // ← 먼저 제거해야 propagateCancel()에서 잔여 세션 체크 정확
                propagateCancel();
            }
        }

        // ── heartbeat ────────────────────────────────────────────────

        void startHeartbeat() {
            heartbeatTask = scheduler.scheduleAtFixedRate(
                    this::sendPing, HEARTBEAT_SEC, HEARTBEAT_SEC, TimeUnit.SECONDS);
        }

        private void stopHeartbeat() {
            if (heartbeatTask != null) heartbeatTask.cancel(false);
        }

        private void sendPing() {
            try {
                emitter.send(SseEmitter.event()
                        .name("ping")
                        .data("{\"alive\":true,\"sessionId\":\"" + sessionId + "\"}"));
            } catch (Exception e) {
                System.out.println("[AiHandler] ping 실패 → 클라이언트 끊김. sid=" + sessionId);
                cancel();
            }
        }

        // ── 취소 전파 ─────────────────────────────────────────────────

        private void propagateCancel() {
            System.out.println("[AiHandler] 취소 전파. sid=" + sessionId + " type=" + type);

            // ① HTTP 소켓 즉시 끊기
            HttpURLConnection conn = activeConn;
            if (conn != null) {
                conn.disconnect();
                System.out.println("[AiHandler] activeConn disconnect 완료");
            }

            // ② Ollama 언로드 — 동일 타입 세션이 남아있으면 스킵
            //    (sessions 맵에서 자신은 이미 제거된 상태)
            boolean sameTypeActive = sessions.values().stream()
                    .anyMatch(s -> s.type == this.type);

            if (sameTypeActive) {
                System.out.println("[AiHandler] 동일 타입 세션 활성 중 → Ollama 언로드 스킵. type=" + type);
            } else {
                // lastLoadedType을 null로 초기화 (언로드 후 VRAM 비어있음)
                SessionType loadedType = lastLoadedType;
                lastLoadedType = null;
                if (loadedType != null) {
                    String model = (loadedType == SessionType.EXCEL) ? excelModel : chatModel;
                    System.out.println("[AiHandler] 취소로 인한 Ollama 언로드. model=" + model);
                    daemonRun("cancel-ollama-" + loadedType.name().toLowerCase(),
                            () -> unloadModel(model));
                } else {
                    System.out.println("[AiHandler] VRAM 비어있음 → 언로드 스킵.");
                }
            }

            // ③ ComfyUI 중단 (CHAT 이미지 생성 중인 경우)
            if (type == SessionType.CHAT) {
                daemonRun("cancel-comfy", this::cancelComfy);
            }

            // ④ FastAPI 엑셀 취소 (EXCEL + taskId 등록된 경우)
            if (type == SessionType.EXCEL && excelTaskId != null) {
                daemonRun("cancel-fastapi", this::cancelFastApi);
            }
        }

        /** ComfyUI: /interrupt + /queue clear */
        private void cancelComfy() {
            try {
                HttpURLConnection intr = openPostConn(comfyUrl + "/interrupt");
                intr.setConnectTimeout(5_000);
                intr.setReadTimeout(5_000);
                System.out.println("[AiHandler] ComfyUI interrupt. HTTP " + intr.getResponseCode());

                var body = mapper.createObjectNode();
                body.put("clear", true);
                HttpURLConnection q = openPostConn(comfyUrl + "/queue");
                q.setConnectTimeout(5_000);
                q.setReadTimeout(5_000);
                try (OutputStream os = q.getOutputStream()) {
                    os.write(mapper.writeValueAsBytes(body));
                }
                System.out.println("[AiHandler] ComfyUI queue clear. HTTP " + q.getResponseCode());
            } catch (Exception e) {
                System.err.println("[AiHandler] ComfyUI 취소 실패: " + e.getMessage());
            }
        }

        /** FastAPI: POST /cancel/{taskId} */
        private void cancelFastApi() {
            try {
                String url = pandasServerUrl + "/cancel/" + excelTaskId;
                HttpURLConnection c = openPostConn(url);
                c.setConnectTimeout(5_000);
                c.setReadTimeout(5_000);
                System.out.println("[AiHandler] FastAPI cancel. HTTP " + c.getResponseCode()
                        + " taskId=" + excelTaskId);
            } catch (Exception e) {
                System.err.println("[AiHandler] FastAPI 취소 실패: " + e.getMessage());
            }
        }

        private HttpURLConnection openPostConn(String url) throws Exception {
            HttpURLConnection c = (HttpURLConnection) URI.create(url).toURL().openConnection();
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/json");
            c.setDoOutput(true);
            return c;
        }

        private void daemonRun(String name, Runnable task) {
            Thread t = new Thread(task, name);
            t.setDaemon(true);
            t.start();
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  공개 API
    // ════════════════════════════════════════════════════════════════

    /**
     * Ollama 스트리밍 완료 후 호출. "지금 이 타입의 모델이 VRAM에 올라와 있다"고 기록.
     * Chat → 완료해도 언로드 안 하고 그냥 VRAM에 둔다.
     */
    public void markVramLoaded(SessionType type) {
        lastLoadedType = type;
        System.out.println("[AiHandler] VRAM 점유 기록. type=" + type);
    }

    /**
     * ComfyUI / Excel 등 Chat 이외의 작업 시작 직전에 호출.
     * <p>
     * 규칙:
     * <ul>
     *   <li>VRAM에 다른 타입(예: CHAT)이 올라와 있으면 → 즉시 언로드 후 반환.</li>
     *   <li>이미 같은 타입이거나 아무것도 없으면 → 언로드 스킵.</li>
     * </ul>
     *
     * @param requiredType 곧 사용할 타입 (EXCEL 또는 CHAT)
     */
    public void prepareVramFor(SessionType requiredType) {
        SessionType current = lastLoadedType;
        if (current == null || current == requiredType) {
            System.out.println("[AiHandler] prepareVramFor: 언로드 불필요. current=" + current
                    + " required=" + requiredType);
            lastLoadedType = requiredType;
            return;
        }
        // 타입이 다를 때만 언로드
        String model = (current == SessionType.EXCEL) ? excelModel : chatModel;
        System.out.println("[AiHandler] prepareVramFor: 언로드 시작. current=" + current
                + " → required=" + requiredType + " model=" + model);
        unloadModel(model);
        lastLoadedType = requiredType;
    }

    /** Ollama keep_alive=0 언로드 (동기, 최대 8초 대기) */
    private void unloadModel(String model) {
        try {
            var body = mapper.createObjectNode();
            body.put("model", model);
            body.put("keep_alive", 0);
            HttpURLConnection c = (HttpURLConnection)
                    URI.create(ollamaApiUrl + "/generate").toURL().openConnection();
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/json");
            c.setDoOutput(true);
            c.setConnectTimeout(5_000);
            c.setReadTimeout(8_000);
            try (OutputStream os = c.getOutputStream()) {
                os.write(mapper.writeValueAsBytes(body));
            }
            System.out.println("[AiHandler] unloadModel 완료. model=" + model
                    + " HTTP=" + c.getResponseCode());
        } catch (Exception e) {
            System.err.println("[AiHandler] unloadModel 실패: " + e.getMessage());
        }
    }

    /**
     * 새 세션 생성 + heartbeat 시작.
     *
     * @param emitter   SSE 에미터
     * @param sessionId 세션 ID (null 이면 UUID 자동 생성)
     * @param type      CHAT | EXCEL
     */
    public SessionContext createSession(SseEmitter emitter, String sessionId, SessionType type) {
        String sid = (sessionId == null || sessionId.isBlank())
                ? UUID.randomUUID().toString() : sessionId;

        SessionContext ctx = new SessionContext(sid, emitter, type);
        sessions.put(sid, ctx);
        ctx.startHeartbeat();
        System.out.println("[AiHandler] 세션 생성. sid=" + sid + " type=" + type);
        return ctx;
    }

    /**
     * 외부(컨트롤러 중지 버튼)에서 세션 취소.
     */
    public void cancelSession(String sessionId) {
        SessionContext ctx = sessions.get(sessionId);
        if (ctx != null) {
            System.out.println("[AiHandler] 외부 cancel. sid=" + sessionId);
            ctx.cancel();
        }
    }

    /** 현재 활성 세션 수 (모니터링용) */
    public int activeSessionCount() { return sessions.size(); }
}