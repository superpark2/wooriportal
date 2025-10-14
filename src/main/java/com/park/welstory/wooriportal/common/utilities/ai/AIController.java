package com.park.welstory.wooriportal.common.utilities.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@Controller
@RestController
public class AIController {

    private final AiService aiService;

    @PostMapping("/gemini")
    public AiDTO.GeminiResponse askGemini(@RequestBody AiDTO.GeminiRequest request) {
        return aiService.askGemini(request);
    }

}
