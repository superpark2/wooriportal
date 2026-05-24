package com.mrpark.dev.wooriportal.ai.mcp.tools.embedding;

import com.mrpark.dev.wooriportal.ai.AiHandler;
import com.mrpark.dev.wooriportal.ai.mcp.McpTool;
import com.mrpark.dev.wooriportal.ai.mcp.dto.ToolDefinitionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * MCP 도구: 내부 지식 베이스 검색 (RAG).
 *
 * execute()는 no-op — WebSearchTool과 동일하게
 * AiService 에서 "rag_search" 분기를 직접 처리한다.
 *
 * 자동 등록: @Component → McpToolRegistry 가 수집.
 */
@Component
@RequiredArgsConstructor
public class EmbeddingTool implements McpTool {

    private final EmbeddingService embeddingService;

    @Override
    public String getName() {
        return "rag_search";
    }

    @Override
    public ToolDefinitionDTO getDefinition() {
        return ToolDefinitionDTO.builder()
                .function(ToolDefinitionDTO.FunctionDefinition.builder()
                        .name(getName())
                        .description("""
                                내부 지식 베이스에서 관련 문서를 검색한다.
                                회사 규정, 업무 매뉴얼, 제품 정보 등 사전에 등록된
                                내부 문서 내용이 필요할 때 사용한다.
                                웹 검색이 필요하지 않은 사내 정보 질문에 우선 사용할 것.
                                """)
                        .parameters(ToolDefinitionDTO.ParameterSchema.builder()
                                .properties(Map.of(
                                        "query", ToolDefinitionDTO.PropertySchema.builder()
                                                .type("string")
                                                .description("검색할 질문 또는 키워드")
                                                .build()
                                ))
                                .required(List.of("query"))
                                .build())
                        .build())
                .build();
    }

    /** no-op: AiService 가 rag_search 를 직접 처리한다. */
    @Override
    public void execute(String argumentsJson, String sessionId,
                        AiHandler.SessionContext ctx, SseEmitter emitter) {
        System.out.println("[EmbeddingTool] execute() 직접 호출됨 — AiService 라우팅 확인 필요. sid=" + sessionId);
    }

    /**
     * AiService 에서 직접 호출.
     * @return 컨텍스트 문자열 (없으면 빈 문자열)
     */
    public String fetchContext(String query) {
        return embeddingService.retrieveContext(query);
    }
}
