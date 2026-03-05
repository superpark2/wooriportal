package com.park.welstory.wooriportal.global.utilities.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class AIController {

    private final AiService aiService;

    @PostMapping("/gemini")
    public AiDTO.GeminiResponse askGemini(@RequestBody AiDTO.GeminiRequest request) {
        return aiService.askGemini(request);
    }

}
