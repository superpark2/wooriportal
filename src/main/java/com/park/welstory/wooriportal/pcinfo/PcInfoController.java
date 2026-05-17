package com.park.welstory.wooriportal.pcinfo;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Controller
@Log4j2
public class PcInfoController {

    private final PcInfoService pcinfoService;

    // 버그수정: 하드코딩된 비밀번호 "2004"를 application.properties로 외부화
    @Value("${pcinfo.verify.password}")
    private String verifyPassword;

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
        return ResponseEntity.ok(Map.of("success", true, "pcInfoNum", saved.getPcInfoNum()));
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
    public ResponseEntity<Map<String, Object>> verifyPassword(@RequestBody String password) {
        return ResponseEntity.ok(Map.of("success", verifyPassword.equals(password)));
    }
}
