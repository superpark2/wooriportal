package com.park.welstory.wooriportal.ai.mcp.tools.websearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.park.welstory.wooriportal.ai.AiHandler;
import com.park.welstory.wooriportal.ai.mcp.McpTool;
import com.park.welstory.wooriportal.ai.mcp.dto.ToolDefinitionDTO;
import com.park.welstory.wooriportal.ai.mcp.tools.websearch.dto.WebSearchToolArgumentDTO;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCP 도구: 웹 검색 (DuckDuckGo).
 *
 * execute() 는 검색 결과를 SSE token 이벤트로 직접 스트리밍한다.
 * AiService 가 결과를 받아 LLM에 다시 넘기는 2-turn 방식 대신,
 * 검색 결과를 바로 사용자에게 전달하는 단순 모드로 동작한다.
 *
 * (필요 시 AiService 콜백 방식으로 확장 가능)
 */
@Component
public class WebSearchTool implements McpTool {

    @Override
    public String getName() {
        return "web_search";
    }

    @Override
    public ToolDefinitionDTO getDefinition() {
        return ToolDefinitionDTO.builder()
                .function(ToolDefinitionDTO.FunctionDefinition.builder()
                        .name(getName())
                        .description("""
                                웹에서 최신 정보를 검색한다.
                                날씨, 뉴스, 주가, 환율 등 실시간 정보가 필요하거나
                                사용자가 검색을 명시적으로 요청할 때 사용한다.
                                """)
                        .parameters(ToolDefinitionDTO.ParameterSchema.builder()
                                .properties(Map.of(
                                        "query", ToolDefinitionDTO.PropertySchema.builder()
                                                .type("string")
                                                .description("검색할 키워드 또는 질문")
                                                .build()
                                ))
                                .required(List.of("query"))
                                .build())
                        .build())
                .build();
    }

    @Override
    public void execute(String argumentsJson, String sessionId,
                        AiHandler.SessionContext ctx, SseEmitter emitter) {
        try {
            WebSearchToolArgumentDTO args = MAPPER.readValue(argumentsJson, WebSearchToolArgumentDTO.class);
            String query = args.getQuery();

            sendToken(emitter, "🔍 웹에서 검색하고 있어요...\n");

            if (ctx.isCancelled()) return;

            List<String> results = search(query);

            if (results.isEmpty()) {
                sendToken(emitter, "검색 결과를 찾지 못했어요.\n");
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < results.size(); i++) {
                    sb.append(i + 1).append(". ").append(results.get(i)).append("\n");
                }
                sendToken(emitter, sb.toString());
            }

            emitter.send(SseEmitter.event().name("done")
                    .data("{\"fullContent\":" + MAPPER.writeValueAsString(
                            results.isEmpty() ? "검색 결과 없음" : String.join("\n", results)
                    ) + "}"));
            emitter.complete();

        } catch (Exception e) {
            System.err.println("[WebSearchTool] 오류: " + e.getMessage());
            sendError(emitter, "검색 중 오류가 발생했습니다.");
        }
    }

    // ── 검색 로직 ─────────────────────────────────────────────────

    private List<String> search(String query) {
        List<String> results = new ArrayList<>(searchJson(query));
        if (results.size() < 3) {
            results.addAll(searchHtml(query, 5 - results.size()));
        }
        return results;
    }

    private List<String> searchJson(String query) {
        List<String> results = new ArrayList<>();
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url     = "https://api.duckduckgo.com/?q=" + encoded
                    + "&format=json&no_redirect=1&no_html=1&skip_disambig=1&kl=kr-ko";

            HttpURLConnection conn = openGet(url);
            if (conn.getResponseCode() != 200) return results;

            String body = readBody(conn);
            JsonNode root = MAPPER.readTree(body);

            String answer       = root.path("Answer").asText("").trim();
            String abstractText = root.path("AbstractText").asText("").trim();
            String abstractUrl  = root.path("AbstractURL").asText("").trim();

            if (!answer.isEmpty())       results.add("즉답: " + answer);
            if (!abstractText.isEmpty()) results.add(abstractText
                    + (abstractUrl.isEmpty() ? "" : " (출처: " + abstractUrl + ")"));

            JsonNode topics = root.path("RelatedTopics");
            if (topics.isArray()) {
                for (JsonNode topic : topics) {
                    String text = topic.path("Text").asText("").trim();
                    String url2 = topic.path("FirstURL").asText("").trim();
                    if (!text.isEmpty()) {
                        results.add(text + (url2.isEmpty() ? "" : " (" + url2 + ")"));
                    }
                    if (results.size() >= 5) break;
                }
            }
        } catch (Exception e) {
            System.err.println("[WebSearchTool] JSON 검색 오류: " + e.getMessage());
        }
        return results;
    }

    private List<String> searchHtml(String query, int max) {
        List<String> results = new ArrayList<>();
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url     = "https://html.duckduckgo.com/html/?q=" + encoded + "&kl=kr-ko";

            HttpURLConnection conn = openGet(url);
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setRequestProperty("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8");
            if (conn.getResponseCode() != 200) return results;

            String html = readBody(conn);
            Pattern titlePat   = Pattern.compile("class=\"result__a\"[^>]*>(.*?)</a>",   Pattern.DOTALL);
            Pattern snippetPat = Pattern.compile("class=\"result__snippet\"[^>]*>(.*?)</a>", Pattern.DOTALL);

            List<String> titles   = extractAll(titlePat,   html);
            List<String> snippets = extractAll(snippetPat, html);

            int count = Math.min(max, Math.min(titles.size(), snippets.size()));
            for (int i = 0; i < count; i++) {
                results.add(titles.get(i) + ": " + snippets.get(i));
            }
        } catch (Exception e) {
            System.err.println("[WebSearchTool] HTML 검색 오류: " + e.getMessage());
        }
        return results;
    }

    // ── HTTP 유틸 ─────────────────────────────────────────────────

    private HttpURLConnection openGet(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setConnectTimeout(8_000);
        conn.setReadTimeout(12_000);
        return conn;
    }

    private String readBody(HttpURLConnection conn) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private List<String> extractAll(Pattern pattern, String input) {
        List<String> list = new ArrayList<>();
        Matcher m = pattern.matcher(input);
        while (m.find()) list.add(stripHtml(m.group(1)));
        return list;
    }

    private String stripHtml(String html) {
        return html.replaceAll("<[^>]+>", "")
                .replaceAll("&amp;", "&").replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">").replaceAll("&quot;", "\"")
                .replaceAll("&#39;", "'").replaceAll("&nbsp;", " ").trim();
    }

    // ── SSE 유틸 ──────────────────────────────────────────────────

    private void sendToken(SseEmitter emitter, String text) {
        try {
            emitter.send(SseEmitter.event().name("token")
                    .data("{\"token\":" + MAPPER.writeValueAsString(text) + "}"));
        } catch (Exception ignored) {}
    }

    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error")
                    .data("{\"message\":\"" + message + "\"}"));
            emitter.complete();
        } catch (Exception ignored) {}
    }
}
