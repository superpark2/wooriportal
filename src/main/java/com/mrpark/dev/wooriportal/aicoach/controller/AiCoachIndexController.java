package com.mrpark.dev.wooriportal.aicoach.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * /aicoach 인덱스 컨트롤러
 * - GET /aicoach → 메인 페이지 반환
 */
@Controller
public class AiCoachIndexController {

    @GetMapping("/aicoach")
    public String index() {
        return "aicoach/index";
    }
}
