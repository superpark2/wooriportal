package com.mrpark.dev.wooriportal.ai.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrpark.dev.wooriportal.ai.AiHandler;
import com.mrpark.dev.wooriportal.ai.mcp.dto.ToolDefinitionDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * MCP 도구 인터페이스.
 *
 * 구현 규칙:
 *  - @Component 붙여서 Spring Bean으로 등록 → McpToolRegistry가 자동 수집
 *  - getDefinition()에서 Ollama tools 배열에 들어갈 도구 스펙 반환
 *  - execute()에서 arguments JSON 문자열을 자신의 ArgumentDTO로 역직렬화 후 실행
 */
public interface McpTool {

    ObjectMapper MAPPER = new ObjectMapper();

    /** 도구 이름 (Ollama tool_calls.function.name 과 일치해야 함) */
    String getName();

    /** Ollama tools 배열에 포함될 도구 정의 */
    ToolDefinitionDTO getDefinition();

    /**
     * LLM이 이 도구를 선택했을 때 실행.
     *
     * @param argumentsJson tool_calls.function.arguments JSON 문자열
     * @param sessionId     세션 ID
     * @param ctx           SSE 세션 컨텍스트
     * @param emitter       SSE 에미터
     */
    void execute(String argumentsJson, String sessionId,
                 AiHandler.SessionContext ctx, SseEmitter emitter);
}
