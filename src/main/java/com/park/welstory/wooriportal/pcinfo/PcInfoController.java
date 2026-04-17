package com.park.welstory.wooriportal.pcinfo;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Controller
@Log4j2
public class PcInfoController {

    private final PcInfoService pcinfoService;

    @GetMapping("/facility/pcinfo/list")
    public String pcinfoView(HttpServletRequest request, Model model) {
        model.addAttribute("title", "시설장비");
        model.addAttribute("subTitle", "PC관리");
        request.setAttribute("activeMenu", "facility");
        request.setAttribute("activeSubMenu", "facilitypcinfo");
        return "common/pcinfo";
    }

    @GetMapping("/pcinfo/view/{pcInfoNum}")
    public String pcinfoMobileView(@PathVariable Long pcInfoNum, Model model) {
        try {
            PcInfoDTO pc = pcinfoService.getById(pcInfoNum);
            model.addAttribute("pc", pc);
            model.addAttribute("buildingNum", pc.getLocationNum());
        } catch (Exception e) {
            model.addAttribute("error", "PC 정보를 찾을 수 없습니다. (ID: " + pcInfoNum + ")");
        }
        return "common/pcinfoview";
    }

    @GetMapping("/facility/pcinfo/pclist")
    @ResponseBody
    public List<PcInfoDTO> pcinfoList(Long location) {
        return pcinfoService.getList(location);
    }

    @PostMapping("/facility/pcinfo/add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> pcInfoAdd(
            PcInfoDTO pcInfoDTO,
            @RequestParam(required = false) String imageDelete) {
        PcInfoEntity saved = pcinfoService.pcInfoAdd(pcInfoDTO, imageDelete);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("pcInfoNum", saved.getPcInfoNum()); // 프론트에서 pcInfoNum 받아갈 수 있도록
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/facility/pcinfo/delete")
    public ResponseEntity<String> pcInfoDeleteById(@RequestParam Long pcInfoNum) {
        try {
            pcinfoService.pcInfoDelete(pcInfoNum);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("삭제 실패");
        }
    }

    @PostMapping("/facility/pcinfo/verify-password")
    @ResponseBody
    public ResponseEntity<Object> verifyPassword(@RequestBody String password) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", password.equals("2004"));
        return ResponseEntity.ok(response);
    }
}