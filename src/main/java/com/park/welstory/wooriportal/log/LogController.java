package com.park.welstory.wooriportal.log;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/qrlog")
@RequiredArgsConstructor
public class LogController {

    private final LogService qrLogService;

    // QR 로그 목록 페이지
    @GetMapping("/log/list")
    public String listPage(Model model) {
        // 기본값으로 최근 6개월 설정
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(6);
        
        model.addAttribute("startDate", startDate.toString());
        model.addAttribute("endDate", endDate.toString());
        
        return "log/list";
    }


    @GetMapping("/logs")
    @ResponseBody
    public Map<String, Object> getQrLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<LogDTO> logPage = qrLogService.getRecentQrLogs(PageRequest.of(page, size));
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", logPage.getContent());
        response.put("totalPages", logPage.getTotalPages());
        response.put("totalElements", logPage.getTotalElements());
        response.put("currentPage", page);
        response.put("hasNext", logPage.hasNext());
        response.put("hasPrevious", logPage.hasPrevious());
        
        return response;
    }


    @GetMapping("/log/pcinfo/{pcinfoNum}")
    @ResponseBody
    public Map<String, Object> getPcQrLogs(
            @PathVariable Long pcinfoNum,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<LogDTO> logPage = qrLogService.getQrLogsByPcinfoNum(pcinfoNum, page, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", logPage.getContent());
        response.put("totalPages", logPage.getTotalPages());
        response.put("totalElements", logPage.getTotalElements());
        response.put("currentPage", page);
        response.put("hasNext", logPage.hasNext());
        response.put("hasPrevious", logPage.hasPrevious());
        
        return response;
    }


    @GetMapping("/api/pc/{pcinfoNum}")
    @ResponseBody
    public Map<String, Object> getPcLogsForMobile(
            @PathVariable Long pcinfoNum,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        Page<LogDTO> logPage = qrLogService.getQrLogsByPcinfoNum(pcinfoNum, page, size);
        Map<String, Object> response = new HashMap<>();
        response.put("content", logPage.getContent());
        response.put("totalPages", logPage.getTotalPages());
        response.put("totalElements", logPage.getTotalElements());
        response.put("currentPage", page);
        response.put("hasNext", logPage.hasNext());
        response.put("hasPrevious", logPage.hasPrevious());
        return response;
    }

    @PostMapping("/api/pc/{pcinfoNum}")
    @ResponseBody
    public Map<String, Object> addPcLogForMobile(@PathVariable Long pcinfoNum, @RequestBody Map<String, String> body) {
        String content = body.get("content");
        qrLogService.savePcQrLog(pcinfoNum, content);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }


    @PostMapping("/log/pcinfo/{pcinfoNum}")
    @ResponseBody
    public Map<String, Object> savePcQrLog(@PathVariable Long pcinfoNum, @RequestBody Map<String, String> body) {
        String content = body.get("content");
        qrLogService.savePcQrLog(pcinfoNum, content);
        Map<String, Object> result = new HashMap<>();
        result.put("result", "success");
        return result;
    }


    // QR 로그 삭제 API
    @DeleteMapping("/log/{logNum}")
    @ResponseBody
    public Map<String, Object> deleteQrLog(@PathVariable Long qrlogNum) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            qrLogService.deleteQrLog(qrlogNum);
            result.put("success", true);
            result.put("message", "QR 로그가 삭제되었습니다.");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        
        return result;
    }
}