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
 * │                                                                      │
 * │  [수정 내역]                                                          │
 * │  1. imageStatusMessage() 단일 통합 (hasNewAttach 파라미터 추가)        │
 * │     - 기존 imageStatusMessage(int) / imageStatusMessageWithNewAttach() │
 * │       두 메서드를 하나로 합침. 규칙 중복 제거.                          │
 * │  2. buildMessages()에서 두 메서드 분기 → 단일 호출로 단순화            │
 * │  3. systemMessage() reminder에서 imageOrder 규칙 제거                 │
 * │     - 규칙은 imageStatusMessage() 한 곳에만 존재                      │
 * │  4. IMAGE_GEN 분기에서 세션 이미지 사전 삭제 제거                      │
 * │     - LLM이 IMAGE_GEN 선택 → imagesForGen=[] 만 설정                 │
 * │     - 세션 이미지 정리는 AiImgService.generate() 완료 후 책임         │
 * └──────────────────────────────────────────────────────────────────────┘
 */
@Service
@RequiredArgsConstructor
public class AiService {

    private final AiHandler      aiHandler;
    private final AiSessionStore sessionStore;
    private final AiImgService   aiImgService;

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
        String skin      = request.getSkin();

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

        // 신규 첨부 이미지가 있을 때 세션 allImages 교체
        // lastImageB64는 "ComfyUI가 만든 생성 결과"만 담으므로 여기서 건드리지 않는다.
        if (isNewlyAttached) {
            sessionStore.putAllImages(sessionId, attachedImages);
        }

        route(userMessage, attachedImages, isNewlyAttached, sessionId, skin, ctx, ctx.getEmitter());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  라우터
    // ══════════════════════════════════════════════════════════════════════════

    private void route(String userMessage, List<String> attachedImages,
                       boolean isNewlyAttached,
                       String sessionId, String skin,
                       AiHandler.SessionContext ctx, SseEmitter emitter) {
        try {
            if (ctx.isCancelled()) return;

            if (isWebSearch(userMessage)) {
                handleWebSearch(userMessage, sessionId, skin, ctx, emitter);
                return;
            }

            List<String> sessionImages = sessionStore.getAllImages(sessionId);

            // ── [버그 수정] allImages 비었는데 lastImageFile은 있는 경우 ──────────
            // b64 캐싱 실패 후 putLastImageFile만 호출된 불일치 상태.
            // resolveSessionImage()로 b64를 재로드해 sessionAllImages 복구.
            if (!isNewlyAttached && sessionImages.isEmpty()) {
                String resolved = aiImgService.resolveSessionImage(sessionId);
                if (resolved != null) {
                    sessionStore.putAllImages(sessionId, List.of(resolved));
                    sessionImages = List.of(resolved);
                    System.out.println("[AI Router] sid=" + sessionId + " sessionAllImages 복구 완료 (resolveSessionImage)");
                }
            }

            List<String> availableImages = isNewlyAttached ? attachedImages : sessionImages;

            System.out.println("[AI Router] sid=" + sessionId
                    + " 신규첨부=" + attachedImages.size() + "장"
                    + " 세션이미지=" + sessionImages.size() + "장"
                    + " → handleChat (LLM 자율 판단)");

            handleChat(userMessage, availableImages, isNewlyAttached, sessionId, skin, ctx, emitter);

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
        streamOllama(sessionId, null, userMessage, availableImages, isUserAttached, skin, ctx, emitter);
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
            streamOllama(sessionId, contextPrompt, userMessage, List.of(), false, skin, ctx, emitter);
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
                              List<String> availableImages,
                              boolean isUserAttached,
                              String skin,
                              AiHandler.SessionContext ctx, SseEmitter emitter) {

        List<AIDTO.OllamaRequest.Message> messages = buildMessages(sessionId, overrideLastUser, skin, isUserAttached);
        AIDTO.OllamaRequest req = new AIDTO.OllamaRequest();
        req.setModel(chatModel);
        req.setMessages(messages);
        req.setStream(true);
        req.setOptions(new AIDTO.OllamaRequest.Options(2048, 8192));

        StringBuilder full = new StringBuilder();
        try {
            HttpURLConnection conn = openOllamaConn();
            ctx.setActiveConn(conn);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(mapper.writeValueAsBytes(req));
            }

            boolean       inThink  = false;
            boolean       inAction = false;
            boolean       inResult = false;
            StringBuilder tagAccum = new StringBuilder();
            StringBuilder completedActionTag = new StringBuilder();
            int           actionBraceDepth = 0;

            final String ACTION_OPEN      = "[IMAGE_ACTION:";
            final String ACTION_OPEN_NOBR = "IMAGE_ACTION:";
            final String RESULT_OPEN      = "[IMAGE_RESULT:";

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
                                if (!inThink && !inAction && !inResult) {
                                    tagAccum.append(c);
                                    String ta = tagAccum.toString();

                                    if (ta.endsWith(ACTION_OPEN) || ACTION_OPEN.startsWith(ta)) {
                                        if (ta.equals(ACTION_OPEN)) {
                                            inAction = true;
                                            tagAccum.setLength(0);
                                        }
                                    } else if (ta.endsWith(ACTION_OPEN_NOBR) || ACTION_OPEN_NOBR.startsWith(ta)) {
                                        if (ta.equals(ACTION_OPEN_NOBR)) {
                                            inAction = true;
                                            tagAccum.setLength(0);
                                        }
                                    } else if (ta.endsWith(RESULT_OPEN) || RESULT_OPEN.startsWith(ta)) {
                                        if (ta.equals(RESULT_OPEN)) {
                                            inResult = true;
                                            tagAccum.setLength(0);
                                        }
                                    } else if (ta.equals("<think>")) {
                                        inThink = true;
                                        tagAccum.setLength(0);
                                    } else if ("<think>".startsWith(ta)) {
                                        // 부분 매칭 → 계속 누적
                                    } else {
                                        toSend.append(ta);
                                        tagAccum.setLength(0);
                                    }

                                } else if (inResult) {
                                    if (c == ']') {
                                        inResult = false;
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
                            if (tagAccum.length() > 0 && !inThink && !inAction && !inResult) {
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
                    "\\[?IMAGE_ACTION:\\s*(\\{.*?\\})\\]?", Pattern.DOTALL);
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
            finalContent = finalContent.replaceAll("(?s)\\[IMAGE_RESULT:[^]]*]", "").trim();

            // ── IMAGE_ACTION 발동 → AiImgService 위임 ────────────────────────
            if (imageAction != null) {
                String intent            = imageAction.path("intent").asText("IMAGE_GEN");
                String prompt            = imageAction.path("prompt").asText("");
                String stage2Prompt      = imageAction.path("stage2Prompt").isNull()
                        ? null : imageAction.path("stage2Prompt").asText(null);
                List<Integer> imageOrder = parseImageOrder(imageAction);

                System.out.println("[AiService] IMAGE_ACTION 감지: intent=" + intent
                        + " prompt=" + prompt
                        + " stage2Prompt=" + stage2Prompt
                        + " imageOrder=" + imageOrder
                        + " isUserAttached=" + isUserAttached);

                // IMAGE_ACTION 태그를 finalContent와 함께 메모리에 저장
                try {
                    String actionHint = "[IMAGE_ACTION:{\"intent\":\"" + intent
                            + "\",\"prompt\":" + mapper.writeValueAsString(prompt)
                            + ",\"stage2Prompt\":" + (stage2Prompt == null ? "null" : mapper.writeValueAsString(stage2Prompt))
                            + ",\"imageOrder\":[],\"imageSize\":null}]";
                    String memContent = (finalContent.isBlank() ? "" : finalContent + "\n") + actionHint;
                    addToMemory(sessionId, "assistant", memContent, null);
                } catch (Exception e) {
                    if (!finalContent.isBlank()) addToMemory(sessionId, "assistant", finalContent, null);
                }

                List<String> imagesForGen;

                if ("IMAGE_GEN".equals(intent)) {
                    // ── [수정 4] 신규 생성: 세션 이미지 사전 삭제 제거 ────────────
                    // 기존: sessionStore.removeLastImageFile/B64/AllImages 후 List.of()
                    // 수정: LLM이 IMAGE_GEN을 선택했으므로 imagesForGen=[] 만 설정.
                    //       세션 이미지 정리는 AiImgService.generate() 완료 콜백에서 처리.
                    imagesForGen = List.of();

                } else {
                    // ── IMAGE_EDIT: 어떤 이미지를 넘길지 LLM의 imageOrder로 판단 ──
                    //
                    // [케이스 A] 유저가 새 이미지를 첨부한 경우
                    //   imageOrder에 신규 첨부 범위를 벗어난 인덱스 포함
                    //     → LLM이 세션 이미지도 필요하다고 판단한 것 (합성/페이스스왑 등)
                    //     → 신규 이미지 + resolveSessionImage() 추가
                    //   imageOrder가 신규 첨부 범위 안에만 있음
                    //     → LLM이 신규 이미지만으로 충분하다고 판단
                    //     → 신규 이미지만 단독 편집
                    //
                    // [케이스 B] 유저가 새 이미지를 첨부하지 않은 경우
                    //     → 세션 이미지(이전 생성 결과)만 사용
                    //
                    if (isUserAttached) {
                        boolean llmWantsSessionImg = imageOrder.stream()
                                .anyMatch(o -> o >= availableImages.size());

                        if (llmWantsSessionImg) {
                            String lastB64 = aiImgService.resolveSessionImage(sessionId);
                            if (lastB64 != null) {
                                List<String> merged = new ArrayList<>(availableImages);
                                merged.add(lastB64);
                                imagesForGen = merged;
                                System.out.println("[AiService] 합성 모드: 신규 " + availableImages.size()
                                        + "장 + 세션 이미지 1장 → 총 " + merged.size() + "장");
                            } else {
                                imagesForGen = new ArrayList<>(availableImages);
                                System.out.println("[AiService] 합성 요청이나 세션 이미지 없음 → 신규 이미지만 사용");
                            }
                        } else {
                            // 단독 편집: 이전 생성 이미지 불필요 → 생성 이미지 상태 원자적 초기화
                            // allImages는 streamChat()에서 신규 첨부로 이미 교체됐으므로 건드리지 않는다.
                            // 기존: removeLastImageB64 + removeLastImageFile 개별 호출
                            //       → allImages 잔류로 다음 턴에 이전 이미지 섞임 버그 발생
                            // 수정: clearGeneratedImages() 단일 호출로 원자적 처리
                            sessionStore.clearGeneratedImages(sessionId);
                            imagesForGen = new ArrayList<>(availableImages);
                            System.out.println("[AiService] 단독 편집 모드: 신규 이미지 " + availableImages.size() + "장만 사용");
                        }
                    } else {
                        // 유저 첨부 없음 → 세션 이미지 그대로 사용
                        imagesForGen = (availableImages != null) ? new ArrayList<>(availableImages) : List.of();
                        System.out.println("[AiService] 세션 이미지 편집: " + imagesForGen.size() + "장");
                    }
                }

                aiImgService.generate(prompt, stage2Prompt, imagesForGen, imageOrder,
                        originMsg, sessionId, ctx, emitter);
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

    private void replaceLastAssistantMemory(String sessionId, String newContent) {
        Deque<AIDTO.OllamaRequest.Message> deque = sessionMemory.get(sessionId);
        if (deque == null || deque.isEmpty()) {
            addToMemory(sessionId, "assistant", newContent, null);
            return;
        }
        List<AIDTO.OllamaRequest.Message> list = new ArrayList<>(deque);
        for (int i = list.size() - 1; i >= 0; i--) {
            if ("assistant".equals(list.get(i).getRole())) {
                list.get(i).setContent(newContent);
                deque.clear();
                deque.addAll(list);
                return;
            }
        }
        addToMemory(sessionId, "assistant", newContent, null);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  메시지 빌드
    // ══════════════════════════════════════════════════════════════════════════

    private List<AIDTO.OllamaRequest.Message> buildMessages(String sessionId, String overrideLastUser,
                                                            String skin, boolean isUserAttached) {
        List<AIDTO.OllamaRequest.Message> messages = new ArrayList<>();
        Deque<AIDTO.OllamaRequest.Message> deque = sessionMemory.get(sessionId);
        if (deque != null) messages.addAll(deque);

        // images가 빈 리스트인 경우 null 처리 (원본 deque 불변 유지)
        messages = messages.stream().map(m -> {
            if (m.getImages() != null && m.getImages().isEmpty()) {
                AIDTO.OllamaRequest.Message copy = new AIDTO.OllamaRequest.Message();
                copy.setRole(m.getRole());
                copy.setContent(m.getContent());
                copy.setImages(null);
                return copy;
            }
            return m;
        }).collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        if (overrideLastUser != null) {
            for (int i = messages.size() - 1; i >= 0; i--) {
                if ("user".equals(messages.get(i).getRole())) {
                    AIDTO.OllamaRequest.Message original = messages.get(i);
                    AIDTO.OllamaRequest.Message replaced = new AIDTO.OllamaRequest.Message();
                    replaced.setRole(original.getRole());
                    replaced.setContent(overrideLastUser);
                    if (original.getImages() != null) replaced.setImages(original.getImages());
                    messages.set(i, replaced);
                    break;
                }
            }
        }

        // ── 메시지 순서: 히스토리 → IMAGE_STATUS(조건부) → 시스템 → 유저 ──────
        // Gemma는 마지막에 가까운 system을 더 강하게 따르므로
        // 시스템 프롬프트를 현재 user 메시지 바로 앞에 조립한다.

        // ① 현재 user 메시지를 히스토리에서 분리
        AIDTO.OllamaRequest.Message lastUserMsg = null;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).getRole())) {
                lastUserMsg = messages.remove(i);
                break;
            }
        }

        // ── [수정 1+2] imageStatusMessage() 단일 통합 호출 ───────────────────
        //
        // 기존: imageStatusMessage(int) / imageStatusMessageWithNewAttach() 두 메서드로 분기
        // 수정: imageStatusMessage(sessionCount, hasNewAttach) 단일 메서드로 통합.
        //
        // 주입 조건:
        //   - 신규 첨부 없음 + 세션 이미지 있음 → 세션 이미지 정보 주입
        //   - 신규 첨부 있음 + 이전 생성 이미지도 있음 → 두 이미지 모두 알림
        //   - 신규 첨부만 있고 세션 이미지 없음 → IMAGE_STATUS 주입 안 함
        //     (LLM에게 불필요한 세션 이미지 정보를 주지 않아 imageOrder=[1,2] 오발 방지)
        //
        boolean hasPrevGenImage = sessionStore.getLastImageB64(sessionId).isPresent()
                || sessionStore.getLastImageFile(sessionId).isPresent();

        if (!isUserAttached) {
            List<String> sessionImgs = sessionStore.getAllImages(sessionId);
            if (!sessionImgs.isEmpty()) {
                messages.add(imageStatusMessage(sessionImgs.size(), false));
            }
        } else if (hasPrevGenImage) {
            // 신규 첨부 있음 + 이전 생성 이미지도 존재 → 합성 판단 가능하도록 알림
            // sessionCount는 1 고정: lastImageB64/File 기준으로 생성 이미지는 항상 1장.
            // allImages는 이미 신규 첨부로 교체됐으므로 여기서 참조하면 오히려 틀림.
            messages.add(imageStatusMessage(1, true));
        } else {
            // ── [버그 수정] 신규 첨부만 있고 이전 생성 이미지 없음 ───────────────
            // 기존: IMAGE_STATUS 미주입 → LLM이 "세션 이미지 없음"으로 판단 → IMAGE_GEN 오발
            // 수정: 첨부 이미지 존재를 명시해 IMAGE_EDIT 판단 근거 제공
            List<String> sessionImgs = sessionStore.getAllImages(sessionId);
            // streamChat()에서 putAllImages(attached) 이미 호출됐으므로 attached 이미지가 담겨있음
            if (!sessionImgs.isEmpty()) {
                messages.add(imageStatusMessageAttachedOnly(sessionImgs.size()));
            }
        }

        // ② 시스템 프롬프트 주입
        messages.add(systemMessage(skin));

        // ③ 현재 user 메시지를 맨 끝에 복원
        if (lastUserMsg != null) {
            messages.add(lastUserMsg);
        }

        return messages;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  [수정 1] imageStatusMessage() — 단일 통합 메서드
    //
    //  기존: imageStatusMessage(int count)            → 세션 이미지만 있을 때
    //        imageStatusMessageWithNewAttach()         → 신규 첨부 + 세션 이미지
    //  수정: imageStatusMessage(int count, boolean hasNewAttach) 하나로 통합.
    //        규칙 기술이 한 곳에만 존재하므로 LLM이 일관된 기준으로 판단 가능.
    // ══════════════════════════════════════════════════════════════════════════

    private AIDTO.OllamaRequest.Message imageStatusMessage(int sessionCount, boolean hasNewAttach) {
        AIDTO.OllamaRequest.Message msg = new AIDTO.OllamaRequest.Message();
        msg.setRole("system");

        String content;
        if (hasNewAttach) {
            // 신규 첨부 이미지 있음 + 이전 생성 세션 이미지도 존재
            content =
                    "[IMAGE_STATUS: 유저가 새 이미지를 첨부함. 동시에 이전 생성된 세션 이미지 " + sessionCount + "장도 존재함.]\n\n" +
                            "imageOrder 결정 규칙:\n" +
                            "1. 새 이미지만 편집 요청 → imageOrder=[1], intent=IMAGE_EDIT\n" +
                            "2. 새 이미지 얼굴을 이전 이미지에 합성 (페이스스왑 등 두 이미지 모두 필요) → imageOrder=[1,2], intent=IMAGE_EDIT\n" +
                            "   슬롯2에는 세션 이미지가 자동으로 채워진다.\n" +
                            "3. 완전 새 이미지 생성 → intent=IMAGE_GEN";
        } else {
            // 신규 첨부 없음 + 세션 이미지만 존재
            content =
                    "[IMAGE_STATUS: 세션에 이미지 " + sessionCount + "장이 존재함 (이전 생성 결과).]\n\n" +
                            "imageOrder 결정 규칙:\n" +
                            "1. 세션 이미지 편집·수정 요청 → imageOrder=[1], intent=IMAGE_EDIT\n" +
                            "2. 유저가 새 이미지를 첨부하지 않았으므로 imageOrder=[1,2] 사용 불가\n" +
                            "3. 완전 새 이미지 생성 요청 → intent=IMAGE_GEN";
        }

        msg.setContent(content);
        return msg;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  [추가] imageStatusMessageAttachedOnly() — 신규 첨부만 있고 이전 생성 없음
    //
    //  imageStatusMessage(hasNewAttach=false) 와 구분:
    //    false → 세션 이미지(이전 생성 결과)가 있을 때
    //    AttachedOnly → 방금 유저가 첨부한 이미지만 있을 때
    //  규칙을 분리함으로써 LLM이 imageOrder=[1,2] 오발 없이 단독 편집 판단 가능.
    // ══════════════════════════════════════════════════════════════════════════

    private AIDTO.OllamaRequest.Message imageStatusMessageAttachedOnly(int attachedCount) {
        AIDTO.OllamaRequest.Message msg = new AIDTO.OllamaRequest.Message();
        msg.setRole("system");
        msg.setContent(
                "[IMAGE_STATUS: 유저가 새 이미지 " + attachedCount + "장을 첨부함. 이전 생성 세션 이미지 없음.]\n\n" +
                        "imageOrder 결정 규칙:\n" +
                        "1. 첨부 이미지 편집·수정 요청 → imageOrder=[1], intent=IMAGE_EDIT\n" +
                        "2. 완전 새 이미지 생성 요청 → intent=IMAGE_GEN"
        );
        return msg;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  [수정 3] systemMessage() — imageOrder 중복 규칙 제거
    //
    //  기존 reminder에 imageOrder 상세 규칙이 포함되어 있어
    //  imageStatusMessage()와 내용이 중복·충돌했음.
    //  수정: imageOrder 규칙은 imageStatusMessage()에만 기술.
    //        reminder는 "IMAGE_ACTION 출력 의무"만 명시.
    // ══════════════════════════════════════════════════════════════════════════

    private AIDTO.OllamaRequest.Message systemMessage(String skin) {
        String personality = switch (skin == null ? "" : skin) {
            case "ruru"      -> " 반드시 반말로 대답해. 약간 삐딱하고 우울한 말투. 존댓말 절대 금지. 한줄에 17글자만 들어가니 보기좋게.";
            case "silicagel" -> " 반드시 몽환적이고 감각적인 말투로 대답해. 한줄에 17글자만 들어가니 보기좋게.";
            case "maltese"   -> " 반드시 짧고 철학적으로 반말로 대답해. 한줄에 17글자만 들어가니 보기좋게.";
            default          -> " 친근하고 가벼운 존칭. 감정적 언변 가능. 한줄에 17글자만 들어가니 보기좋게. [현재화면]은 대화 맥락 참고용이다. 이미지 작업은 사용자 첨부파일, 사진을 우선시 한다.";
        };

        // imageOrder 세부 규칙은 imageStatusMessage()에만 기술.
        // 여기선 IMAGE_ACTION 출력 의무만 명시하여 중복·충돌 제거.
        String reminder =
                "\n\n[규칙 리마인더]\n" +
                        "이미지 생성·편집 의도가 감지되면 응답 마지막 줄에 반드시 IMAGE_ACTION 태그를 출력하라. 누락 절대 금지.\n" +
                        "[IMAGE_STATUS:] 메시지가 있으면 세션 이미지가 존재한다. 수정·편집 요청 시 즉시 IMAGE_ACTION(intent=IMAGE_EDIT)을 출력하라.\n" +
                        "IMAGE_ACTION 태그는 응답 마지막에 딱 한 번만 출력한다.";

        AIDTO.OllamaRequest.Message msg = new AIDTO.OllamaRequest.Message();
        msg.setRole("system");
        msg.setContent(SYSTEM_PROMPT + personality
                + "\n현재 시간: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                + reminder);
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
        for (int i = request.getMessages().size() - 1; i >= 0; i--) {
            if ("user".equals(request.getMessages().get(i).getRole())) {
                String content = request.getMessages().get(i).getContent().trim();
                int idx = content.indexOf("[질문]\n");
                if (idx != -1) return content.substring(idx + "[질문]\n".length()).trim();
                return content;
            }
        }
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