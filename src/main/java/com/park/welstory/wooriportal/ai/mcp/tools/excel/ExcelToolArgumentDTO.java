package com.park.welstory.wooriportal.ai.mcp.tools.excel.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * MCP ExcelTool 실행 시 LLM이 전달하는 arguments DTO.
 *
 * 직렬화 형태:
 * {
 *   "prompt": "3열 기준 내림차순 정렬해줘"
 * }
 */
@Getter
@NoArgsConstructor
public class ExcelToolArgumentDTO {

    /** 엑셀 처리 요청 내용 */
    private String prompt;
}
