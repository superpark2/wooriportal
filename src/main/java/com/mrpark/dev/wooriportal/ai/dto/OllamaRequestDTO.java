package com.mrpark.dev.wooriportal.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mrpark.dev.wooriportal.ai.mcp.dto.ToolDefinitionDTO;
import lombok.*;

import java.util.List;

/**
 * Ollama /api/chat 요청 DTO.
 * tools 필드를 포함해 MCP 도구 목록을 LLM에 전달한다.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OllamaRequestDTO {

    private String model;
    private List<MessageDTO> messages;
    private boolean stream;
    private Options options;

    /** MCP 도구 목록. null이면 직렬화에서 제외 (일반 채팅) */
    private List<ToolDefinitionDTO> tools;

    // ── 내부 DTO ──────────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MessageDTO {
        private String role;
        private String content;

        /** 이미지 첨부 (Base64 목록). null이면 직렬화 제외 */
        private List<String> images;
    }

    @Getter
    @Builder
    public static class Options {
        @JsonProperty("num_predict")
        private int numPredict;

        @JsonProperty("num_ctx")
        private int numCtx;
    }
}
