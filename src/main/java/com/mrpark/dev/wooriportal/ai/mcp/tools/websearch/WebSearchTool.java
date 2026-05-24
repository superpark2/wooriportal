package com.mrpark.dev.wooriportal.ai.mcp.tools.websearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrpark.dev.wooriportal.ai.AiHandler;
import com.mrpark.dev.wooriportal.ai.mcp.McpTool;
import com.mrpark.dev.wooriportal.ai.mcp.dto.ToolDefinitionDTO;
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

/**
 * MCP 도구: 웹 검색 (SearXNG 로컬 인스턴스).
 *
 * execute() 는 no-op. AiService 가 fetchSearchResult() 를 직접 호출한다.
 */
@Component
public class WebSearchTool implements McpTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** SearXNG 로컬 주소 */
    private static final String SEARXNG_URL = "http://localhost:4499";

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

    /** no-op: AiService 가 web_search 를 직접 처리한다. */
    @Override
    public void execute(String argumentsJson, String sessionId,
                        AiHandler.SessionContext ctx, SseEmitter emitter) {
        System.out.println("[WebSearchTool] execute() 직접 호출됨 — AiService 라우팅 확인 필요. sid=" + sessionId);
    }

    /**
     * 검색 실행 후 결과 문자열 반환.
     * AiService 에서 직접 호출한다.
     */
    public String fetchSearchResult(String query) {
        List<String> results = searchSearXNG(query, 5);
        if (results.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            sb.append(i + 1).append(". ").append(results.get(i)).append("\n");
        }
        return sb.toString().trim();
    }

    // ── SearXNG 검색 ──────────────────────────────────────────────

    private List<String> searchSearXNG(String query, int max) {
        List<String> results = new ArrayList<>();
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = SEARXNG_URL + "/search?q=" + encoded + "&format=json&language=ko-KR";

            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(8_000);
            conn.setRequestProperty("Accept", "application/json");

            int status = conn.getResponseCode();
            if (status != 200) {
                System.err.println("[WebSearchTool] SearXNG HTTP 상태: " + status);
                return results;
            }

            String body = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))
                    .lines().reduce("", String::concat);

            JsonNode root = MAPPER.readTree(body);
            JsonNode hits = root.path("results");

            if (hits.isArray()) {
                for (JsonNode hit : hits) {
                    String title   = hit.path("title").asText("").trim();
                    String content = hit.path("content").asText("").trim();
                    String hitUrl  = hit.path("url").asText("").trim();

                    if (!title.isEmpty() && !content.isEmpty()) {
                        results.add(title + ": " + content
                                + (hitUrl.isEmpty() ? "" : " (" + hitUrl + ")"));
                    }
                    if (results.size() >= max) break;
                }
            }

            System.out.println("[WebSearchTool] SearXNG 결과: " + results.size() + "건");

        } catch (Exception e) {
            System.err.println("[WebSearchTool] SearXNG 검색 오류: " + e.getMessage());
        }
        return results;
    }
}