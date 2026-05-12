package com.park.welstory.wooriportal.ai.mcp.tools.img;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MCP ImageTool 실행 시 LLM이 전달하는 arguments DTO.
 *
 * 직렬화 형태:
 * {
 *   "prompt":       "a futuristic city at night",
 *   "intent":       "IMAGE_GEN" | "IMAGE_EDIT",
 *   "stage2Prompt": "add neon signs",      // 선택
 *   "imageOrder":   [1, 2]                 // 선택 (1-based)
 * }
 */
@Getter
@NoArgsConstructor
public class ImageToolArgumentDTO {

    /** 이미지 생성/편집에 사용할 영어 프롬프트 */
    private String prompt;

    /**
     * 작업 의도.
     * IMAGE_GEN  : 텍스트 프롬프트로 새 이미지 생성
     * IMAGE_EDIT : 세션/첨부 이미지를 기반으로 편집
     */
    private String intent;

    /** 2장 이상 합성 시 2차 스테이지 프롬프트 (선택) */
    private String stage2Prompt;

    /** 편집 시 사용할 이미지 슬롯 순서 (1-based). 예) [1] 또는 [1,2] */
    private List<Integer> imageOrder;

    /** 이전 생성 이미지를 편집에 포함할지 여부. true면 생성 이미지를 마지막 슬롯에 추가. */
    private boolean useGeneratedImage;
}