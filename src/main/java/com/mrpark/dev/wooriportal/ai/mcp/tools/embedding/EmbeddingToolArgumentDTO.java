package com.mrpark.dev.wooriportal.ai.mcp.tools.embedding;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * MCP EmbeddingTool 실행 시 LLM이 전달하는 arguments DTO.
 *
 * 직렬화 형태:
 * {
 *   "query": "휴가 규정 알려줘"
 * }
 */
@Getter
@NoArgsConstructor
public class EmbeddingToolArgumentDTO {

    /** RAG 검색 쿼리 */
    private String query;
}
