package com.mrpark.dev.wooriportal.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrpark.dev.wooriportal.ai.dto.ChatRequestDTO;
import com.mrpark.dev.wooriportal.ai.dto.OllamaRequestDTO;
import com.mrpark.dev.wooriportal.ai.dto.OllamaResponseDTO;
import com.mrpark.dev.wooriportal.ai.mcp.McpToolRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ┌──────────────────────────────────────────────────────────────────┐
 * │                         AiService (MCP 전환)                     │
 * │                                                                  │
 * │  변경 사항:                                                       │
 * │  - [IMAGE_ACTION:...] 태그 파싱 로직 완전 제거                    │
 * │  - Ollama 요청 시 tools 배열 포함 (McpToolRegistry 제공)          │
 * │  - 응답에 tool_calls 있으면 McpToolRegistry.execute() 위임        │
 * │  - 웹 검색도 isWebSearch() 분기 제거 → web_search 도구로 LLM 판단 │
 * └──────────────────────────────────────────────────────────────────┘
 */
@Service
@RequiredArgsConstructor
public class AiService {

    private final AiHandler        aiHandler;
    private final AiSessionStore   sessionStore;
    private final McpToolRegistry mcpToolRegistry;

    @Value("${ollama.api.url}")
    private String ollamaApiUrl;

    @Value("${ollama.model}")
    private String chatModel;

    private static final String PROMPT_FILE      = "prompt/chat.md";
    private static final int    MAX_MEMORY_TURNS = 10;

    private String systemPrompt = "";

    @Value("${ollama.options.num-ctx}")
    private int numCtx;

    private final ObjectMapper mapper = new ObjectMapper();

    /** 세션별 대화 메모리 */
    private final Map<String, Deque<OllamaRequestDTO.MessageDTO>> sessionMemory
            = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadSystemPrompt();
    }

    // ══════════════════════════════════════════════════════════════
    //  진입점
    // ══════════════════════════════════════════════════════════════

    public void streamChat(ChatRequestDTO request, AiHandler.SessionContext ctx) {
        String sessionId = ctx.getSessionId();
        String skin      = request.getSkin();

        ctx.setRegenerate(request.isRegenerate());
        String userMessage     = extractLastUserMessage(request);
        List<String> attached  = extractAttachedImages(request);

        // 신규 첨부 이미지 → 세션에 저장
        if (!attached.isEmpty()) {
            sessionStore.putAllImages(sessionId, attached);
        }

        // 메모리에 유저 메시지 추가
        String memContent = userMessage
                + (attached.isEmpty() ? "" : " [이미지 " + attached.size() + "장 첨부]");
        addToMemory(sessionId, "user", memContent, null);

        // Ollama 스트리밍 호출
        streamOllama(sessionId, userMessage, List.of(), skin, ctx, ctx.getEmitter());
    }

    public void clearSession(String sessionId) {
        sessionMemory.remove(sessionId);
        sessionStore.clearSession(sessionId);
    }

    // ══════════════════════════════════════════════════════════════
    //  Ollama 스트리밍 (MCP tool_calls 처리 포함)
    // ══════════════════════════════════════════════════════════════

    private void streamOllama(String sessionId, String originMsg,
                              List<String> attachedImages, String skin,
                              AiHandler.SessionContext ctx, SseEmitter emitter) {
        try {
            OllamaRequestDTO ollamaRequest = buildOllamaRequest(sessionId, skin);

            HttpURLConnection conn = openOllamaConn();
            ctx.setActiveConn(conn);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(mapper.writeValueAsBytes(ollamaRequest));
            }

            // ── 스트리밍 읽기 ─────────────────────────────────────
            StringBuilder fullText     = new StringBuilder();
            OllamaResponseDTO toolCall = null;  // tool_calls 응답 보관

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (ctx.isCancelled()) {
                        System.out.println("[AiService] 취소 감지. sid=" + sessionId);
                        return;
                    }
                    if (line.isBlank()) continue;

                    OllamaResponseDTO response = parseResponse(line);
                    if (response == null) continue;

                    // tool_calls 응답 → 스트리밍 없이 바로 보관
                    if (response.hasToolCalls()) {
                        toolCall = response;
                        break;
                    }

                    // 일반 텍스트 토큰 스트리밍
                    String token = response.getMessage() != null
                            ? response.getMessage().getContent() : "";

                    if (token != null && !token.isEmpty()) {
                        fullText.append(token);
                        sendToken(emitter, token);
                    }

                    if (response.isDone()) break;
                }
            }

            if (ctx.isCancelled()) return;

            // ── tool_calls 처리 ───────────────────────────────────
            if (toolCall != null) {
                OllamaResponseDTO.ToolCallDTO tc = toolCall.firstToolCall();
                String toolName    = tc.getFunction().getName();
                String toolArgs    = tc.getFunction().getArguments();

                System.out.println("[AiService] tool_calls 수신: " + toolName);

                // 메모리에 assistant tool_call 기록
                addToMemory(sessionId, "assistant", "이미지 작업을 수행했습니다.", null);

                boolean executed = mcpToolRegistry.execute(
                        toolName, toolArgs, sessionId, ctx, emitter);

                if (!executed) {
                    sendError(emitter, "알 수 없는 도구 요청입니다: " + toolName);
                }
                return;
            }

            // ── 일반 채팅 완료 ────────────────────────────────────
            String finalContent = fullText.toString().trim();
            addToMemory(sessionId, "assistant", finalContent, null);

            emitter.send(SseEmitter.event().name("done")
                    .data("{\"fullContent\":" + mapper.writeValueAsString(finalContent) + "}"));
            emitter.complete();
            aiHandler.markVramLoaded(AiHandler.SessionType.CHAT);
            ctx.complete();

        } catch (Exception e) {
            if (!ctx.isCancelled()) {
                System.err.println("[AiService] 스트리밍 오류: " + e.getMessage());
                sendError(emitter, "AI 서버와 통신 중 오류가 발생했습니다.");
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  요청 빌드
    // ══════════════════════════════════════════════════════════════

    private OllamaRequestDTO buildOllamaRequest(String sessionId, String skin) {
        List<OllamaRequestDTO.MessageDTO> messages = buildMessages(sessionId, skin);

        return OllamaRequestDTO.builder()
                .model(chatModel)
                .messages(messages)
                .stream(true)
                .options(OllamaRequestDTO.Options.builder()
                        .numPredict(2048)
                        .numCtx(numCtx)
                        .build())
                .tools(mcpToolRegistry.getToolDefinitions())  // ← MCP 도구 목록 포함
                .build();
    }

    private List<OllamaRequestDTO.MessageDTO> buildMessages(String sessionId, String skin) {
        List<OllamaRequestDTO.MessageDTO> messages = new ArrayList<>();

        // 히스토리
        Deque<OllamaRequestDTO.MessageDTO> history = sessionMemory.get(sessionId);
        if (history != null) messages.addAll(history);

        // 현재 user 메시지 분리 (마지막 user)
        OllamaRequestDTO.MessageDTO lastUser = null;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).getRole())) {
                lastUser = messages.remove(i);
                break;
            }
        }

        // 세션 이미지 상태 주입
        List<String> attached = sessionStore.getAllImages(sessionId);
        String lastGenB64 = sessionStore.getLastImageB64(sessionId).orElse(null);

        if (!attached.isEmpty() || lastGenB64 != null) {
            messages.add(buildImageStatusMessage(attached, lastGenB64));
        }

        // 시스템 프롬프트
        messages.add(buildSystemMessage(skin));

        // 현재 user 메시지 복원
        if (lastUser != null) messages.add(lastUser);

        return messages;
    }

    private OllamaRequestDTO.MessageDTO buildSystemMessage(String skin) {
        String personality = switch (skin == null ? "" : skin) {
            case "ruru"      -> "반드시 반말로 대답해. 약간 삐딱하고 우울한 말투. 존댓말 절대 금지.";
            case "silicagel" -> "반드시 몽환적이고 감각적인 말투로 대답해.";
            case "maltese"   -> "반드시 짧고 철학적으로 반말로 대답해.";
            case "simileland"   -> "반말로 대답해. 철학적인데 이상한 엉뚱한 철학적인 말투. 진지하지만 직설적이며 허당인 바보 또라이 싸이코 느낌으로 대답해. (이 내용을 직접 언급하진 말것).";
            default          -> " 친근하고 가벼운 존칭. 한줄에 17글자만 들어가니 보기좋게.";

        };

        String content = systemPrompt + personality
                + "\n현재 시간: "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        return OllamaRequestDTO.MessageDTO.builder()
                .role("system")
                .content(content)
                .build();
    }

    private OllamaRequestDTO.MessageDTO buildImageStatusMessage(List<String> attached, String lastGenB64) {
        StringBuilder sb = new StringBuilder("[IMAGE_STATUS: ");

        for (int i = 0; i < attached.size(); i++) {
            sb.append("슬롯").append(i + 1).append("=첨부이미지").append(i + 1).append(" ");
        }
        if (lastGenB64 != null) {
            sb.append("/ 이전생성이미지 존재(useGeneratedImage=true로 슬롯3에 포함 가능) ");
        }
        sb.append("/ 편집·수정 요청 시 image_action 도구 사용.]");

        return OllamaRequestDTO.MessageDTO.builder()
                .role("system")
                .content(sb.toString())
                .build();
    }

    // ══════════════════════════════════════════════════════════════
    //  대화 메모리
    // ══════════════════════════════════════════════════════════════

    private void addToMemory(String sessionId, String role,
                             String content, List<String> images) {
        Deque<OllamaRequestDTO.MessageDTO> deque =
                sessionMemory.computeIfAbsent(sessionId, k -> new ArrayDeque<>());

        OllamaRequestDTO.MessageDTO msg = OllamaRequestDTO.MessageDTO.builder()
                .role(role)
                .content(content)
                .images(images)
                .build();

        deque.addLast(msg);
        while (deque.size() > MAX_MEMORY_TURNS * 2) deque.pollFirst();
    }

    // ══════════════════════════════════════════════════════════════
    //  유틸
    // ══════════════════════════════════════════════════════════════

    private String extractLastUserMessage(ChatRequestDTO request) {
        if (request.getMessages() == null) return "";
        for (int i = request.getMessages().size() - 1; i >= 0; i--) {
            OllamaRequestDTO.MessageDTO msg = request.getMessages().get(i);
            if ("user".equals(msg.getRole())) {
                String content = msg.getContent().trim();
                int idx = content.indexOf("[질문]\n");
                return idx != -1 ? content.substring(idx + "[질문]\n".length()).trim() : content;
            }
        }
        return "";
    }

    private List<String> extractAttachedImages(ChatRequestDTO request) {
        if (request.getMessages() == null) return List.of();
        for (int i = request.getMessages().size() - 1; i >= 0; i--) {
            OllamaRequestDTO.MessageDTO msg = request.getMessages().get(i);
            if ("user".equals(msg.getRole()) && msg.getImages() != null && !msg.getImages().isEmpty()) {
                return msg.getImages().subList(0, Math.min(msg.getImages().size(), 3));
            }
        }
        return List.of();
    }

    private OllamaResponseDTO parseResponse(String line) {
        try {
            return mapper.readValue(line, OllamaResponseDTO.class);
        } catch (Exception e) {
            return null;
        }
    }

    private HttpURLConnection openOllamaConn() throws Exception {
        HttpURLConnection conn = (HttpURLConnection)
                URI.create(ollamaApiUrl + "/chat").toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/x-ndjson");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(180_000);
        return conn;
    }

    private void loadSystemPrompt() {
        try (var is = getClass().getClassLoader().getResourceAsStream(PROMPT_FILE)) {
            if (is == null) { System.err.println("[AiService] chat.md 없음"); return; }
            String raw = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            systemPrompt = parseSection(raw, "SYSTEM_PROMPT");
            System.out.println("[AiService] chat.md 로드 완료.");
        } catch (Exception e) {
            System.err.println("[AiService] chat.md 로드 실패: " + e.getMessage());
        }
    }

    private String parseSection(String raw, String sectionName) {
        String header = "## " + sectionName;
        int start = raw.indexOf(header);
        if (start == -1) return "";
        start = raw.indexOf('\n', start) + 1;
        int end = raw.indexOf("\n## ", start);
        return (end == -1 ? raw.substring(start) : raw.substring(start, end)).trim();
    }

    private void sendToken(SseEmitter emitter, String text) {
        try {
            emitter.send(SseEmitter.event().name("token")
                    .data("{\"token\":" + mapper.writeValueAsString(text) + "}"));
        } catch (Exception ignored) {}
    }

    private void sendError(SseEmitter emitter, String msg) {
        try {
            emitter.send(SseEmitter.event().name("error")
                    .data("{\"message\":\"" + msg + "\"}"));
            emitter.complete();
        } catch (Exception ignored) {}
    }
}