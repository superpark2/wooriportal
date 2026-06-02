package com.mrpark.dev.wooriportal.hrd.board;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 전광판 진단(머신/로그인 없이 폴 결과 확인). {@code /coolapi/**} 라 permitAll.
 */
@RestController
@RequestMapping("/coolapi/hrd/board")
@RequiredArgsConstructor
public class HrdBoardDiagnosticController {

    private final HrdBoardService boardService;

    @GetMapping("/status")
    public Map<String, Object> status() {
        return boardService.status();
    }
}
