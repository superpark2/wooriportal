package com.mrpark.dev.wooriportal.ai.mcp.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Ollama tools 배열 한 항목.
 *
 * 직렬화 결과:
 * {
 *   "type": "function",
 *   "function": {
 *     "name": "generate_image",
 *     "description": "...",
 *     "parameters": {
 *       "type": "object",
 *       "properties": {
 *         "prompt": { "type": "string", "description": "..." },
 *         "intent": { "type": "string", "enum": ["IMAGE_GEN","IMAGE_EDIT"] }
 *       },
 *       "required": ["prompt", "intent"]
 *     }
 *   }
 * }
 */
@Getter
@Builder
public class ToolDefinitionDTO {

    @Builder.Default
    private String type = "function";

    private FunctionDefinition function;

    // ── 내부 DTO ──────────────────────────────────────────────────

    @Getter
    @Builder
    public static class FunctionDefinition {
        private String name;
        private String description;
        private ParameterSchema parameters;
    }

    @Getter
    @Builder
    public static class ParameterSchema {
        @Builder.Default
        private String type = "object";

        /** 파라미터명 → PropertySchema */
        private Map<String, PropertySchema> properties;

        private List<String> required;
    }

    @Getter
    @Builder
    public static class PropertySchema {
        private String type;
        private String description;

        /** enum 제약이 있는 경우 (없으면 null → 직렬화 제외) */
        private List<String> enumValues;
    }
}
