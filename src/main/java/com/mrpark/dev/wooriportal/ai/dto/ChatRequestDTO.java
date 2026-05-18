package com.mrpark.dev.wooriportal.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 프론트엔드 → AiController 채팅 요청 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatRequestDTO {

    private List<OllamaRequestDTO.MessageDTO> messages;
    private String sessionId;
    private String editImageUrl;

    /** UI 스킨 (ruru / silicagel / maltese / 기본) */
    private String skin;

    private boolean regenerate;
}
