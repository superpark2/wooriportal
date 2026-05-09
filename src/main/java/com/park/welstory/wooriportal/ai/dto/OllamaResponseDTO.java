package com.park.welstory.wooriportal.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.List;

/**
 * Ollama /api/chat 스트리밍 응답 DTO.
 *
 * 일반 텍스트 응답:
 *   message.content 에 토큰이 담김
 *
 * tool_calls 응답 (MCP 도구 선택 시):
 *   message.toolCalls[0].function.name      → 도구 이름
 *   message.toolCalls[0].function.arguments → 도구 인자
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaResponseDTO {

    private MessageDTO message;
    private boolean done;

    public boolean hasToolCalls() {
        return message != null
                && message.getToolCalls() != null
                && !message.getToolCalls().isEmpty();
    }

    public ToolCallDTO firstToolCall() {
        return message.getToolCalls().get(0);
    }

    // ── 내부 DTO ──────────────────────────────────────────────────

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessageDTO {
        private String role;
        private String content;

        @JsonProperty("tool_calls")
        private List<ToolCallDTO> toolCalls;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ToolCallDTO {
        private FunctionCallDTO function;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FunctionCallDTO {
        private String name;

        /**
         * arguments는 도구마다 구조가 다르므로 String으로 받아
         * 각 Tool 구현체에서 자신의 ArgumentDTO로 역직렬화한다.
         */
        @JsonDeserialize(using = ArgumentsDeserializer.class)
        private String arguments;
    }

    static class ArgumentsDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctx)
                throws IOException {
            if (p.currentToken() == JsonToken.START_OBJECT) {
                return p.readValueAsTree().toString();
            }
            return p.getText();
        }
    }
}
