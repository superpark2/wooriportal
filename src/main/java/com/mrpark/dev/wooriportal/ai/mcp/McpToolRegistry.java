package com.mrpark.dev.wooriportal.ai.mcp;

import com.mrpark.dev.wooriportal.ai.AiHandler;
import com.mrpark.dev.wooriportal.ai.mcp.dto.ToolDefinitionDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MCP 도구 레지스트리.
 *
 * Spring DI로 McpTool 구현체 전부 자동 수집.
 * 새 도구 추가 시 tools/ 에 @Component 붙인 구현체만 만들면 자동 등록.
 */
@Component
@RequiredArgsConstructor
public class McpToolRegistry {

    private final List<McpTool> tools;

    private Map<String, McpTool> toolMap;

    @PostConstruct
    public void init() {
        toolMap = tools.stream()
                .collect(Collectors.toMap(McpTool::getName, Function.identity()));
        System.out.println("[McpToolRegistry] 등록된 도구: " + toolMap.keySet());
    }

    /**
     * Ollama /api/chat 요청에 포함할 도구 정의 목록 반환.
     * OllamaRequestDTO.tools 에 그대로 세팅하면 됨.
     */
    public List<ToolDefinitionDTO> getToolDefinitions() {
        return tools.stream()
                .map(McpTool::getDefinition)
                .collect(Collectors.toList());
    }

    /**
     * LLM의 tool_calls 응답을 받아 해당 도구 실행.
     *
     * @param toolName      tool_calls.function.name
     * @param argumentsJson tool_calls.function.arguments (JSON 문자열)
     * @return 도구 찾아서 실행했으면 true, 없으면 false
     */
    public boolean execute(String toolName, String argumentsJson,
                           String sessionId,
                           AiHandler.SessionContext ctx,
                           SseEmitter emitter) {
        McpTool tool = toolMap.get(toolName);
        if (tool == null) {
            System.err.println("[McpToolRegistry] 알 수 없는 도구: " + toolName);
            return false;
        }
        System.out.println("[McpToolRegistry] 실행: " + toolName + " args=" + argumentsJson);
        tool.execute(argumentsJson, sessionId, ctx, emitter);
        return true;
    }
}
