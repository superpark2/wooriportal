package com.park.welstory.wooriportal.pcinfo;

import com.park.welstory.wooriportal.location.LocationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Controller
@Log4j2
public class PcInfoController {

    private final PcinfoService pcinfoService;
    private final LocationService locationService; // reserved for future use

    @GetMapping("/facility/pcinfo/list")
    public String pcinfoView(HttpServletRequest request, Model model){
        model.addAttribute("title", "시설장비");
        model.addAttribute("subTitle", "PC관리");
        request.setAttribute("activeMenu", "facility");
        request.setAttribute("activeSubMenu", "facilitypcinfo");
        return "common/pcinfo";
    }

    // 모바일/QR 전용 상세보기
    @GetMapping("/pcinfo/view/{pcInfoNum}")
    public String pcinfoMobileView(@PathVariable Long pcInfoNum, Model model) {
        try {
            PcInfoDTO pc = pcinfoService.getById(pcInfoNum);
            model.addAttribute("pc", pc);
            model.addAttribute("buildingNum", pc.getLocationNum());
            return "common/pcinfoview";
        } catch (Exception e) {
            model.addAttribute("error", "PC 정보를 찾을 수 없습니다. (ID: " + pcInfoNum + ")");
            return "common/pcinfoview";
        }
    }

    @GetMapping("/facility/pcinfo/pclist")
    @ResponseBody
    public List<PcInfoDTO> pcinfoList(Long location){
        return pcinfoService.getList(location);
    }

    @PostMapping(value = "/facility/pcinfo/add")
    public ResponseEntity<String> pcInfoAdd(PcInfoDTO pcInfoDTO,
                                            @RequestParam(required = false) String imageDelete){
        pcinfoService.pcInfoAdd(pcInfoDTO, imageDelete);
        return ResponseEntity.ok("success");
    }

    @DeleteMapping("/facility/pcinfo/delete")
    public ResponseEntity<String> pcInfoDeleteById(@RequestParam Long pcInfoNum) {
        pcinfoService.pcInfoDelete(pcInfoNum);
        return ResponseEntity.ok("success");
    }
}
