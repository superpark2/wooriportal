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

    private final LogService LogService;

    @GetMapping("/facility/log/list")
    public String listPage(Model model, HttpServletRequest request, Pageable pageable) {

        model.addAttribute("title", "시설장비");
        model.addAttribute("subTitle", "Log");
        request.setAttribute("activeMenu", "facility");
        request.setAttribute("activeSubMenu", "log");

        model.addAttribute("logs", LogService.getRecentLogs(pageable));

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(12);
        model.addAttribute("startDate", startDate.toString());
        model.addAttribute("endDate", endDate.toString());

        
        return "common/log";
    }


    @GetMapping("/log/logs")
    @ResponseBody
    public Map<String, Object> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<LogDTO> logPage = LogService.getRecentLogs(PageRequest.of(page, size));
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", logPage.getContent());
        response.put("totalPages", logPage.getTotalPages());
        response.put("totalElements", logPage.getTotalElements());
        response.put("currentPage", page);
        response.put("hasNext", logPage.hasNext());
        response.put("hasPrevious", logPage.hasPrevious());
        
        return response;
    }


    @GetMapping("/log/pc/{pcinfoNum}")
    @ResponseBody
    public Map<String, Object> getPcLogsForMobile(
            @PathVariable Long pcinfoNum,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        // 특정 PC의 로그만 조회
        Page<LogDTO> logPage = LogService.getLogsByPcInfoNum(pcinfoNum, PageRequest.of(page, size));
        Map<String, Object> response = new HashMap<>();
        response.put("content", logPage.getContent());
        response.put("totalPages", logPage.getTotalPages());
        response.put("totalElements", logPage.getTotalElements());
        response.put("currentPage", page);
        response.put("hasNext", logPage.hasNext());
        response.put("hasPrevious", logPage.hasPrevious());
        return response;
    }

    @PostMapping("/log/pc/{pcinfoNum}")
    @ResponseBody
    public Map<String, Object> addPcLogForMobile(@PathVariable Long pcinfoNum, @RequestBody Map<String, String> body) {
        String content = body.get("content");
        LogService.processLog(pcinfoNum, content, "등록");
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }


    @PostMapping("/log/pcinfo/{pcinfoNum}")
    @ResponseBody
    public Map<String, Object> savePcLog(@PathVariable Long pcinfoNum, @RequestBody Map<String, String> body) {
        String content = body.get("content");
        LogService.processLog(pcinfoNum, content, "저장");
        Map<String, Object> result = new HashMap<>();
        result.put("result", "success");
        return result;
    }


    // QR 로그 삭제 API
    @DeleteMapping("/log/{logNum}")
    @ResponseBody
    public Map<String, Object> deleteLog(@PathVariable Long logNum) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            LogService.deleteLog(logNum);
            result.put("success", true);
            result.put("message", "로그가 삭제되었습니다.");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        
        return result;
    }
}