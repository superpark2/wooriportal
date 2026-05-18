package com.mrpark.dev.wooriportal.ai.mcp.tools.websearch.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * MCP WebSearchTool 실행 시 LLM이 전달하는 arguments DTO.
 *
 * 직렬화 형태:
 * {
 *   "query": "오늘 서울 날씨"
 * }
 */
@Getter
@NoArgsConstructor
public class WebSearchToolArgumentDTO {

    /** 검색할 키워드 또는 질문 */
    private String query;
}
