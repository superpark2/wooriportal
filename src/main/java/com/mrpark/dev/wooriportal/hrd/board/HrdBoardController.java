package com.mrpark.dev.wooriportal.hrd.board;

import com.mrpark.dev.wooriportal.hrd.dto.HrdAttendee;
import com.mrpark.dev.wooriportal.hrd.dto.HrdCourseDetail;
import com.mrpark.dev.wooriportal.hrd.dto.HrdDailyAttendance;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 실시간 출결 전광판: 화면 + SSE 스트림 + 초기 데이터(JSON). */
@Controller
@RequestMapping("/hrd/board")
@RequiredArgsConstructor
public class HrdBoardController {

    private final HrdBoardService boardService;

    @GetMapping
    public String board(@RequestParam(value = "demo", required = false) String demo, Model model) {
        model.addAttribute("demo", demo != null);
        return "hrd/board";
    }

    /** 초기 로드용 스냅샷(JSON). demo=1 이면 샘플 데이터. */
    @GetMapping("/data")
    @ResponseBody
    public List<HrdBoardRow> data(@RequestParam(value = "demo", required = false) String demo) {
        if (demo != null) {
            return demoRows();
        }
        return boardService.snapshot();
    }

    /** 실시간 갱신 스트림(SSE). */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter stream() {
        return boardService.subscribe();
    }

    // ── 미리보기용 샘플(HRD 미연동 상태에서 UI 확인) ──

    private List<HrdBoardRow> demoRows() {
        return List.of(
                demo("전산회계1급 자격증", "2026-04-02~2026-06-05 (17회차)", "19:00~22:00(총 3시간)",
                        new String[][]{{"강건임", "실업자", "출석", "in"}, {"송유나", "재직자", "출석", "in"},
                                {"이몽룡", "실업자", "지각", "in"}, {"성춘향", "실업자", "결석", ""}}),
                demo("AWS기반 웹서비스플랫폼개발", "2025-12-31~2026-06-26 (8회차)", "09:00~18:00(총 8시간)",
                        new String[][]{{"홍길동", "실업자", "출석", "out"}, {"임꺽정", "실업자", "출석", "out"},
                                {"장길산", "실업자", "출석", "out"}}));
    }

    private HrdBoardRow demo(String name, String period, String time, String[][] people) {
        HrdCourseDetail c = new HrdCourseDetail();
        c.setTracseNm(name);
        c.setTracsePd(period);
        c.setTraingTime(time);
        c.setTraingBeginTime("0900");
        c.setTraingEndTime("1800");
        List<HrdAttendee> roster = new java.util.ArrayList<>();
        for (String[] p : people) {
            HrdAttendee a = new HrdAttendee();
            a.setCstmrNm(p[0]);
            a.setTrneeSeNm(p[1]);
            if ("out".equals(p[3])) {
                a.setCheckInTime("0900");
                a.setCheckOutTime("1800");
            } else if ("in".equals(p[3])) {
                a.setCheckInTime("0905");
            }
            roster.add(a);
        }
        return new HrdBoardRow(new HrdDailyAttendance(c, roster), true, java.time.LocalTime.now(), java.util.List.of(), null);
    }
}
