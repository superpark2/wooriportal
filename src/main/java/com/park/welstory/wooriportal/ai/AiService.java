package com.park.welstory.wooriportal.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.park.welstory.wooriportal.ai.dto.AIDTO;
import com.park.welstory.wooriportal.ai.img.AiImgService;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │                          AiService                                   │
 * │  Ollama 채팅 스트리밍·웹 검색 전담 서비스.                              │
 * │                                                                      │
 * │  이미지 생성·편집은 AiImgService에 완전히 위임한다.                      │
 * │  세션 이미지 상태는 AiSessionStore를 통해 AiImgService와 공유한다.      │
 * │                                                                      │
 * │  ┌─────────────────────────────────────────────────────────────┐    │
 * │  │  streamChat()                                                │    │
 * │  │    └─ route()                                               │    │
 * │  │         ├─ 웹 검색 → handleWebSearch()                       │    │
 * │  │         └─ 일반   → handleChat() → streamOllama()           │    │
 * │  │              LLM이 IMAGE_ACTION 태그를 반환하면               │    │
 * │  │              → AiImgService.generate() 위임                  │    │
 * │  └─────────────────────────────────────────────────────────────┘    │
 * └──────────────────────────────────────────────────────────────────────┘
 */
@Service
@RequiredArgsConstructor
public class AiService {

    private final AiHandler      aiHandler;
    private final AiSessionStore sessionStore;
    private final AiImgService aiImgService;   // 이미지 생성·편집 위임

    @Value("${ollama.api.url}")
    private String ollamaApiUrl;

    @Value("${ollama.model}")
    private String chatModel;

    private static final String PROMPT_FILE      = "prompt/chat.md";
    private static final int    MAX_MEMORY_TURNS = 10;

    private String SYSTEM_PROMPT = "";

    private final ObjectMapper mapper = new ObjectMapper();

    /** 세션별 대화 메모리 */
    private final Map<String, Deque<AIDTO.OllamaRequest.Message>> sessionMemory = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadPrompts();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  프롬프트 로드
    // ══════════════════════════════════════════════════════════════════════════

    private void loadPrompts() {
        try (var is = getClass().getClassLoader().getResourceAsStream(PROMPT_FILE)) {
            if (is == null) {
                System.err.println("[AiService] chat.md 없음: " + PROMPT_FILE);
                return;
            }
            String raw = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            SYSTEM_PROMPT = parseSection(raw, "SYSTEM_PROMPT");
            System.out.println("[AiService] chat.md 로드 완료.");
        } catch (Exception e) {
            System.err.println("[AiService] chat.md 로드 실패: " + e.getMessage());
        }
    }

    /** ## SECTION_NAME 부터 다음 ## 전까지 추출 */
    private String parseSection(String raw, String sectionName) {
        String header = "## " + sectionName;
        int start = raw.indexOf(header);
        if (start == -1) return "";
        start = raw.indexOf('\n', start) + 1;
        int end = raw.indexOf("\n## ", start);
        return (end == -1 ? raw.substring(start) : raw.substring(start, end)).trim();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  진입점
    // ══════════════════════════════════════════════════════════════════════════

    public void streamChat(AIDTO.ChatRequest request, AiHandler.SessionContext ctx) {
        String sessionId = ctx.getSessionId();
        String skin      = request.getSkin(); // ★ 스킨 추출

        String userMessage = extractLastUserMessage(request);

        // 다중 이미지 추출 (최대 3장)
        List<String> attachedImages = extractAttachedImages(request);
        boolean isNewlyAttached = !attachedImages.isEmpty();

        // EditImageUrl fallback (단일 이미지 호환)
        if (attachedImages.isEmpty() && request.getEditImageUrl() != null && !request.getEditImageUrl().isBlank()) {
            String fileName = java.nio.file.Paths.get(request.getEditImageUrl()).getFileName().toString();
            String b64 = aiImgService.loadImageFileAsBase64(fileName);
            if (b64 != null) attachedImages = List.of(b64);
            isNewlyAttached = !attachedImages.isEmpty();
        }

        // 새 첨부 이미지가 있으면 세션 저장
        if (isNewlyAttached) {
            sessionStore.putAllImages(sessionId, attachedImages);
            sessionStore.putLastImageB64(sessionId, attachedImages.get(0));
            sessionStore.removeLastImageFile(sessionId);
        }

        route(userMessage, attachedImages, sessionId, skin, ctx, ctx.getEmitter());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  라우터
    // ══════════════════════════════════════════════════════════════════════════

    private void route(String userMessage, List<String> attachedImages,
                       String sessionId, String skin,
                       AiHandler.SessionContext ctx, SseEmitter emitter) {
        try {
            if (ctx.isCancelled()) return;

            // 웹 검색 분기
            if (isWebSearch(userMessage)) {
                handleWebSearch(userMessage, sessionId, skin, ctx, emitter);
                return;
            }

            // 새 첨부 이미지 세션 갱신
            if (!attachedImages.isEmpty()) {
                sessionStore.putAllImages(sessionId, attachedImages);
                sessionStore.putLastImageB64(sessionId, attachedImages.get(0));
                sessionStore.removeLastImageFile(sessionId);
            }

            List<String> sessionImages   = sessionStore.getAllImages(sessionId);
            List<String> availableImages = attachedImages.isEmpty() ? sessionImages : attachedImages;

            System.out.println("[AI Router] sid=" + sessionId
                    + " 신규첨부=" + attachedImages.size() + "장"
                    + " 세션이미지=" + sessionImages.size() + "장"
                    + " → handleChat (LLM 자율 판단)");

            handleChat(userMessage, availableImages, !attachedImages.isEmpty(), sessionId, skin, ctx, emitter);

        } catch (Exception e) {
            System.err.println("[Router] 예외: " + e.getMessage());
            sendError(emitter, "처리 중 오류가 발생했습니다.");
        }
    }

    private boolean isWebSearch(String msg) {
        if (msg == null) return false;
        return msg.matches(".*(오늘|현재|최신|요즘|지금).*(날씨|뉴스|주가|환율|소식).*")
                || msg.matches(".*(검색해줘|찾아줘|찾아봐줘).*");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  [1] 일반 채팅
    // ══════════════════════════════════════════════════════════════════════════

    private void handleChat(String userMessage, List<String> availableImages,
                            boolean isUserAttached,
                            String sessionId, String skin,
                            AiHandler.SessionContext ctx, SseEmitter emitter) {
        String imageHint = "";
        if (availableImages != null && !availableImages.isEmpty()) {
            imageHint = isUserAttached
                    ? " [이미지 " + availableImages.size() + "장 첨부]"
                    : " [이전에 생성한 이미지 " + availableImages.size() + "장 있음 — 유저가 직접 첨부한 게 아님]";
        }
        addToMemory(sessionId, "user", userMessage + imageHint, null);
        streamOllama(sessionId, null, userMessage, availableImages, skin, ctx, emitter);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  [2] 웹 검색
    // ══════════════════════════════════════════════════════════════════════════

    private void handleWebSearch(String userMessage, String sessionId, String skin,
                                 AiHandler.SessionContext ctx, SseEmitter emitter) {
        try {
            if (ctx.isCancelled()) return;
            sendToken(emitter, "🔍 웹에서 검색하고 있어요...\n");

            String query         = userMessage.replaceAll("(?:검색해줘|찾아줘|찾아봐줘)[!.?~\\s]*$", "").trim();
            List<String> results = duckDuckGoSearch(query);

            if (ctx.isCancelled()) return;

            String contextPrompt;
            if (results.isEmpty()) {
                contextPrompt = "'" + query + "' 검색 결과가 없어. 알고 있는 정보로 답해줘: " + userMessage;
            } else {
                StringBuilder sb = new StringBuilder("다음은 '" + query + "' 검색 결과야:\n\n");
                for (int i = 0; i < results.size(); i++)
                    sb.append(i + 1).append(". ").append(results.get(i)).append("\n");
                sb.append("\n위 내용을 참고해서 친근하게, 출처도 간단히 언급하며 답해줘: ").append(userMessage);
                contextPrompt = sb.toString();
            }

            sendToken(emitter, "\n");
            addToMemory(sessionId, "user", userMessage, null);
            streamOllama(sessionId, contextPrompt, userMessage, List.of(), skin, ctx, emitter);
        } catch (Exception e) {
            System.err.println("[AiService] 웹 검색 오류: " + e.getMessage());
            sendError(emitter, "검색 중 오류가 발생했습니다.");
        }
    }

    private List<String> duckDuckGoSearch(String query) {
        List<String> results = new ArrayList<>();
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String apiUrl  = "https://api.duckduckgo.com/?q=" + encoded
                    + "&format=json&no_redirect=1&no_html=1&skip_disambig=1&kl=kr-ko";
            HttpURLConnection conn = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; WooriBot/1.0)");
            conn.setConnectTimeout(8_000);
            conn.setReadTimeout(12_000);
            if (conn.getResponseCode() == 200) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
                JsonNode root         = mapper.readTree(sb.toString());
                String   answer       = root.path("Answer").asText("").trim();
                String   abstractText = root.path("AbstractText").asText("").trim();
                String   abstractUrl  = root.path("AbstractURL").asText("").trim();
                if (!answer.isEmpty())       results.add("즉답: " + answer);
                if (!abstractText.isEmpty()) results.add(abstractText + (abstractUrl.isEmpty() ? "" : " (출처: " + abstractUrl + ")"));
                JsonNode topics = root.path("RelatedTopics");
                if (topics.isArray()) {
                    for (JsonNode topic : topics) {
                        String text = topic.path("Text").asText("").trim();
                        String url  = topic.path("FirstURL").asText("").trim();
                        if (!text.isEmpty()) results.add(text + (url.isEmpty() ? "" : " (" + url + ")"));
                        if (results.size() >= 5) break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[AiService] DDG JSON API 오류: " + e.getMessage());
        }
        if (results.size() < 3) results.addAll(duckDuckGoHtmlSearch(query, 5 - results.size()));
        return results;
    }

    private List<String> duckDuckGoHtmlSearch(String query, int maxResults) {
        List<String> results = new ArrayList<>();
        try {
            String encoded   = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String searchUrl = "https://html.duckduckgo.com/html/?q=" + encoded + "&kl=kr-ko";
            HttpURLConnection conn = (HttpURLConnection) URI.create(searchUrl).toURL().openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setRequestProperty("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8");
            conn.setConnectTimeout(8_000);
            conn.setReadTimeout(12_000);
            if (conn.getResponseCode() != 200) return results;
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            String html = sb.toString();
            Pattern titlePat   = Pattern.compile("class=\"result__a\"[^>]*>(.*?)</a>",       Pattern.DOTALL);
            Pattern snippetPat = Pattern.compile("class=\"result__snippet\"[^>]*>(.*?)</a>", Pattern.DOTALL);
            Matcher tm = titlePat.matcher(html), sm = snippetPat.matcher(html);
            List<String> titles = new ArrayList<>(), snippets = new ArrayList<>();
            while (tm.find()) titles.add(stripHtml(tm.group(1)));
            while (sm.find()) snippets.add(stripHtml(sm.group(1)));
            int count = Math.min(maxResults, Math.min(titles.size(), snippets.size()));
            for (int i = 0; i < count; i++) results.add(titles.get(i) + ": " + snippets.get(i));
        } catch (Exception e) {
            System.err.println("[AiService] DDG HTML 검색 오류: " + e.getMessage());
        }
        return results;
    }

    private String stripHtml(String html) {
        return html.replaceAll("<[^>]+>", "")
                .replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"").replaceAll("&#39;", "'").replaceAll("&nbsp;", " ").trim();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  [3] Ollama 스트리밍
    //      LLM이 IMAGE_ACTION 태그를 반환하면 AiImgService로 위임
    // ══════════════════════════════════════════════════════════════════════════

    private void streamOllama(String sessionId, String overrideLastUser,
                              String originMsg,
                              List<String> availableImages, String skin,
                              AiHandler.SessionContext ctx, SseEmitter emitter) {
        List<AIDTO.OllamaRequest.Message> messages = buildMessages(sessionId, overrideLastUser, skin);
        AIDTO.OllamaRequest req = new AIDTO.OllamaRequest();
        req.setModel(chatModel);
        req.setMessages(messages);
        req.setStream(true);
        req.setOptions(new AIDTO.OllamaRequest.Options(2048, 4096));

        StringBuilder full = new StringBuilder();
        try {
            HttpURLConnection conn = openOllamaConn();
            ctx.setActiveConn(conn);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(mapper.writeValueAsBytes(req));
            }

            // ── 실시간 태그 필터링 상태 ──────────────────────────────────────
            boolean       inThink  = false;
            boolean       inAction = false;
            StringBuilder tagAccum = new StringBuilder();
            StringBuilder completedActionTag = new StringBuilder();
            int           actionBraceDepth = 0;

            final String ACTION_OPEN      = "[IMAGE_ACTION:";
            final String ACTION_OPEN_NOBR = "IMAGE_ACTION:";
            boolean      actionHadBracket = false;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (ctx.isCancelled()) {
                        System.out.println("[AiService] streamOllama 취소 감지. sid=" + sessionId);
                        break;
                    }
                    if (line.isBlank()) continue;
                    try {
                        JsonNode n        = mapper.readTree(line);
                        String   rawToken = n.path("message").path("content").asText("");
                        boolean  isDone   = n.path("done").asBoolean(false);

                        if (!rawToken.isEmpty()) {
                            StringBuilder toSend = new StringBuilder();
                            for (char c : rawToken.toCharArray()) {
                                if (!inThink && !inAction) {
                                    tagAccum.append(c);
                                    String ta = tagAccum.toString();

                                    // [IMAGE_ACTION: 감지 (정상 패턴)
                                    if (ta.endsWith(ACTION_OPEN) || ACTION_OPEN.startsWith(ta)) {
                                        if (ta.equals(ACTION_OPEN)) {
                                            inAction = true;
                                            actionHadBracket = true;
                                            tagAccum.setLength(0);
                                        }
                                    }
                                    // IMAGE_ACTION: 감지 (대괄호 누락 패턴)
                                    else if (ta.endsWith(ACTION_OPEN_NOBR) || ACTION_OPEN_NOBR.startsWith(ta)) {
                                        if (ta.equals(ACTION_OPEN_NOBR)) {
                                            inAction = true;
                                            actionHadBracket = false;
                                            tagAccum.setLength(0);
                                        }
                                    }
                                    // <think> 감지
                                    else if (ta.equals("<think>")) {
                                        inThink = true;
                                        tagAccum.setLength(0);
                                    } else if ("<think>".startsWith(ta)) {
                                        // 부분 매칭 → 계속 누적
                                    } else {
                                        toSend.append(ta);
                                        tagAccum.setLength(0);
                                    }

                                } else if (inAction) {
                                    tagAccum.append(c);
                                    if      (c == '{') actionBraceDepth++;
                                    else if (c == '}') actionBraceDepth--;
                                    else if (c == ']' && actionBraceDepth == 0) {
                                        inAction = false;
                                        actionBraceDepth = 0;
                                        completedActionTag.append(ACTION_OPEN).append(tagAccum.toString());
                                        tagAccum.setLength(0);
                                    }
                                } else {
                                    // inThink: </think> 감지
                                    tagAccum.append(c);
                                    String ta = tagAccum.toString();
                                    if (ta.endsWith("</think>")) {
                                        inThink = false;
                                        tagAccum.setLength(0);
                                    } else if (ta.length() > "</think>".length()) {
                                        tagAccum.delete(0, tagAccum.length() - "</think>".length());
                                    }
                                }
                            }
                            String filtered = toSend.toString();
                            if (!filtered.isEmpty()) {
                                full.append(filtered);
                                sendToken(emitter, filtered);
                            }
                        }

                        if (isDone) {
                            if (tagAccum.length() > 0 && !inThink && !inAction) {
                                String leftover = tagAccum.toString();
                                full.append(leftover);
                                sendToken(emitter, leftover);
                            }
                            break;
                        }

                    } catch (Exception ignored) {}
                }
            }

            if (ctx.isCancelled()) return;

            // ── IMAGE_ACTION 태그 추출 ────────────────────────────────────────
            String rawFull = full.toString().trim();
            if (completedActionTag.length() > 0) {
                rawFull = rawFull + completedActionTag.toString();
            } else if (inAction && tagAccum.length() > 0) {
                rawFull = rawFull + ACTION_OPEN + tagAccum.toString();
            }

            Pattern actionPat = Pattern.compile(
                    "\\[?IMAGE_ACTION:(\\{.*?\\})\\]?", Pattern.DOTALL);
            Matcher actionMat = actionPat.matcher(rawFull);

            JsonNode imageAction = null;
            String   visibleText = rawFull;
            if (actionMat.find()) {
                try {
                    imageAction = mapper.readTree(actionMat.group(1));
                } catch (Exception e) {
                    System.err.println("[AiService] IMAGE_ACTION JSON 파싱 실패: " + e.getMessage());
                }
                visibleText = rawFull.substring(0, actionMat.start()).stripTrailing();
            }

            String finalContent = extractAfterThink(visibleText);

            // ── IMAGE_ACTION 발동 → AiImgService 위임 ────────────────────────
            if (imageAction != null) {
                String intent         = imageAction.path("intent").asText("IMAGE_GEN");
                String prompt         = imageAction.path("prompt").asText("");
                List<Integer> imageOrder = parseImageOrder(imageAction);

                System.out.println("[AiService] IMAGE_ACTION 감지: intent=" + intent
                        + " prompt=" + prompt + " imageOrder=" + imageOrder);

                if (!finalContent.isBlank()) {
                    addToMemory(sessionId, "assistant", finalContent, null);
                }

                List<String> imagesForGen;
                if ("IMAGE_GEN".equals(intent)) {
                    // 신규 생성: 세션 이미지 초기화
                    sessionStore.removeLastImageFile(sessionId);
                    sessionStore.removeLastImageB64(sessionId);
                    sessionStore.removeAllImages(sessionId);
                    imagesForGen = List.of();
                } else {
                    // 편집: 현재 세션 이미지 사용
                    imagesForGen = (availableImages != null) ? availableImages : List.of();
                }

                // ★ AiImgService에 위임 (originMsg = 유저 원문 → LoRA 키워드 탐지에 사용)
                aiImgService.generate(prompt, imagesForGen, imageOrder,
                        originMsg, sessionId, ctx, emitter);

                // generate() 완료 후 메모리에 기록
                String memMsg  = finalContent.isBlank()
                        ? (imagesForGen.isEmpty() ? "이미지 생성 요청" : "이미지 편집 요청")
                        : finalContent;
                String comment = imagesForGen.isEmpty() ? "이미지를 생성했어요!" : "이미지 편집이 완료됐어요!";
                addToMemory(sessionId, "user",      memMsg,  null);
                addToMemory(sessionId, "assistant", comment, null);
                return;
            }

            // ── 일반 채팅 완료 ────────────────────────────────────────────────
            addToMemory(sessionId, "assistant", finalContent, null);
            emitter.send(SseEmitter.event().name("done")
                    .data("{\"fullContent\":" + mapper.writeValueAsString(finalContent) + "}"));
            emitter.complete();
            aiHandler.markVramLoaded(AiHandler.SessionType.CHAT);
            ctx.complete();

        } catch (Exception e) {
            if (!ctx.isCancelled()) {
                System.err.println("[AiService] SSE 스트리밍 오류: " + e.getMessage());
                sendError(emitter, "AI 서버와 통신 중 오류가 발생했습니다.");
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  대화 메모리
    // ══════════════════════════════════════════════════════════════════════════

    private void addToMemory(String sessionId, String role, String content, List<String> images) {
        Deque<AIDTO.OllamaRequest.Message> deque =
                sessionMemory.computeIfAbsent(sessionId, k -> new ArrayDeque<>());
        AIDTO.OllamaRequest.Message msg = new AIDTO.OllamaRequest.Message();
        msg.setRole(role);
        msg.setContent(content);
        if (images != null && !images.isEmpty()) msg.setImages(images);
        deque.addLast(msg);
        while (deque.size() > MAX_MEMORY_TURNS * 2) deque.pollFirst();
    }

    private List<AIDTO.OllamaRequest.Message> buildMessages(String sessionId, String overrideLastUser, String skin) {
        List<AIDTO.OllamaRequest.Message> messages = new ArrayList<>();
        Deque<AIDTO.OllamaRequest.Message> deque = sessionMemory.get(sessionId);
        if (deque != null) messages.addAll(deque);
        messages.forEach(m -> { if (m.getImages() != null && m.getImages().isEmpty()) m.setImages(null); });

        if (overrideLastUser != null) {
            for (int i = messages.size() - 1; i >= 0; i--) {
                if ("user".equals(messages.get(i).getRole())) {
                    messages.get(i).setContent(overrideLastUser);
                    break;
                }
            }
        }

        // ① 시스템 프롬프트 → 맨 앞에 삽입 (정석)
        messages.add(0, systemMessage(skin));

        // ② 마지막 user 메시지 바로 앞에 리마인더 주입
        int lastUserIdx = -1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).getRole())) {
                lastUserIdx = i;
                break;
            }
        }
        if (lastUserIdx > 0) {
            messages.add(lastUserIdx, reminderMessage());
        }

        return messages;
    }

    private AIDTO.OllamaRequest.Message reminderMessage() {
        AIDTO.OllamaRequest.Message msg = new AIDTO.OllamaRequest.Message();
        msg.setRole("system");
        msg.setContent(
                "시스템 프롬프트의 모든 규칙을 준수하라.\n" +
                        "특히 이미지 규칙: 사용자가 이미지 생성 또는 편집을 요청하는 의도가 감지되면, " +
                        "반드시 응답 마지막 줄에 IMAGE_ACTION 태그를 출력해야 한다. " +
                        "태그 없이 텍스트 응답만 하는 것은 절대 금지다."
        );
        return msg;
    }

    private AIDTO.OllamaRequest.Message systemMessage(String skin) {
        String personality = switch (skin == null ? "" : skin) {
            case "ruru"      -> " 반드시 반말로 대답해. 약간 삐딱하고 우울한 말투. 존댓말 절대 금지. 한줄에 17글자만 들어가니 보기좋게.";
            case "silicagel" -> " 반드시 몽환적이고 감각적인 말투로 대답해. 한줄에 17글자만 들어가니 보기좋게.";
            case "maltese"   -> " 반드시 짧고 철학적으로 반말로 대답해. 한줄에 17글자만 들어가니 보기좋게.";
            default          -> " 친근하고 가벼운 존칭. 감정적 언변 가능. 한줄에 17글자만 들어가니 보기좋게.";
        };
        AIDTO.OllamaRequest.Message msg = new AIDTO.OllamaRequest.Message();
        msg.setRole("system");
        msg.setContent(SYSTEM_PROMPT + personality + "\n현재 시간: "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return msg;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  세션 초기화 (컨트롤러에서 호출)
    // ══════════════════════════════════════════════════════════════════════════

    public void clearSession(String sessionId) {
        sessionMemory.remove(sessionId);
        sessionStore.clearSession(sessionId);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  유틸
    // ══════════════════════════════════════════════════════════════════════════

    private String extractLastUserMessage(AIDTO.ChatRequest request) {
        if (request.getMessages() == null) return "";
        for (int i = request.getMessages().size() - 1; i >= 0; i--)
            if ("user".equals(request.getMessages().get(i).getRole()))
                return request.getMessages().get(i).getContent().trim();
        return "";
    }

    private List<String> extractAttachedImages(AIDTO.ChatRequest request) {
        if (request.getMessages() == null) return List.of();
        for (int i = request.getMessages().size() - 1; i >= 0; i--) {
            AIDTO.OllamaRequest.Message msg = request.getMessages().get(i);
            if ("user".equals(msg.getRole())) {
                List<String> imgs = msg.getImages();
                if (imgs != null && !imgs.isEmpty())
                    return imgs.subList(0, Math.min(imgs.size(), 3));
                return List.of();
            }
        }
        return List.of();
    }

    private List<Integer> parseImageOrder(JsonNode analysis) {
        if (analysis == null) return List.of();
        JsonNode orderNode = analysis.path("imageOrder");
        if (!orderNode.isArray() || orderNode.isEmpty()) return List.of();
        List<Integer> order = new ArrayList<>();
        for (JsonNode n : orderNode) {
            int val = n.asInt(0);
            if (val >= 1) order.add(val - 1); // 1-based → 0-based
        }
        return order;
    }

    private String extractAfterThink(String content) {
        if (content == null) return "";
        int endIdx = content.lastIndexOf("</think>");
        if (endIdx != -1) return content.substring(endIdx + "</think>".length()).trim();
        return content.replaceAll("(?s)<think>.*", "").trim();
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

    private void sendToken(SseEmitter emitter, String text) {
        try {
            emitter.send(SseEmitter.event().name("token")
                    .data("{\"token\":" + mapper.writeValueAsString(text) + "}"));
        } catch (Exception ignored) {}
    }

    private void sendError(SseEmitter emitter, String msg) {
        try {
            emitter.send(SseEmitter.event().name("error").data("{\"message\":\"" + msg + "\"}"));
            emitter.complete();
        } catch (Exception ignored) {}
    }
}