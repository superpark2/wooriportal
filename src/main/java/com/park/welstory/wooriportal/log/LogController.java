package com.park.welstory.wooriportal.log;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    // ── 로그 목록 페이지 ──────────────────────────────────
    @GetMapping("/facility/log/list")
    public String listPage(Model model, HttpServletRequest request, Pageable pageable) {
        model.addAttribute("title", "시설장비");
        model.addAttribute("subTitle", "Log");
        request.setAttribute("activeMenu", "facility");
        request.setAttribute("activeSubMenu", "log");
        model.addAttribute("logs", logService.getRecentLogs(pageable));
        model.addAttribute("startDate", LocalDate.now().minusMonths(12).toString());
        model.addAttribute("endDate", LocalDate.now().toString());
        return "common/log";
    }

    // ── 전체 로그 API ──────────────────────────────────
    @GetMapping("/log/logs")
    @ResponseBody
    public Map<String, Object> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return toPageMap(logService.getRecentLogs(PageRequest.of(page, size)), page);
    }

    // ── 특정 PC 로그 조회 ──────────────────────────────────
    @GetMapping("/log/pc/{pcinfoNum}")
    @ResponseBody
    public Map<String, Object> getPcLogs(
            @PathVariable Long pcinfoNum,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        return toPageMap(logService.getLogsByPcInfoNum(pcinfoNum, PageRequest.of(page, size)), page);
    }

    // ── 수동 로그 등록 (pcinfoview "새 기록" 버튼 / 모바일 QR) ──────────────────────────────────
    // body: { "content": "..." }
    @PostMapping("/log/pc/{pcinfoNum}")
    @ResponseBody
    public Map<String, Object> addPcLog(
            @PathVariable Long pcinfoNum,
            @RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return Map.of("success", false, "message", "내용을 입력하세요.");
        }
        logService.savePcLog(pcinfoNum, content);
        return Map.of("success", true);
    }

    // ── 로그 삭제 ──────────────────────────────────
    @DeleteMapping("/log/{logNum}")
    @ResponseBody
    public Map<String, Object> deleteLog(@PathVariable Long logNum) {
        try {
            logService.deleteLog(logNum);
            return Map.of("success", true, "message", "로그가 삭제되었습니다.");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    private Map<String, Object> toPageMap(Page<LogDTO> logPage, int page) {
        Map<String, Object> res = new HashMap<>();
        res.put("content",       logPage.getContent());
        res.put("totalPages",    logPage.getTotalPages());
        res.put("totalElements", logPage.getTotalElements());
        res.put("currentPage",   page);
        res.put("hasNext",       logPage.hasNext());
        res.put("hasPrevious",   logPage.hasPrevious());
        return res;
    }
}