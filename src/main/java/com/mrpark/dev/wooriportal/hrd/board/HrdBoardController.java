package com.mrpark.dev.wooriportal.hrd.board;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 실시간 출결 전광판: 화면 + SSE 스트림 + 초기 데이터(JSON). */
@Controller
@RequestMapping("/hrd/board")
@RequiredArgsConstructor
public class HrdBoardController {

    private final HrdBoardService boardService;

    @GetMapping
    public String board() {
        return "hrd/board";
    }

    /** 초기 로드용 스냅샷(JSON). */
    @GetMapping("/data")
    @ResponseBody
    public List<HrdBoardRow> data() {
        return boardService.snapshot();
    }

    /** 실시간 갱신 스트림(SSE). */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter stream() {
        return boardService.subscribe();
    }
}
