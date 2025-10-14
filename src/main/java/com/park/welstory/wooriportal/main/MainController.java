package com.park.welstory.wooriportal.main;

import com.park.welstory.wooriportal.common.board.BoardService;
import com.park.welstory.wooriportal.facility.FacilityRepository;
import com.park.welstory.wooriportal.management.ManagementRepository;
import com.park.welstory.wooriportal.pcinfo.pcinfoRequire.PcInfoRequireService;
import com.park.welstory.wooriportal.sales.SalesRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final BoardService boardService;
    private final PcInfoRequireService pcInfoRequireService;
    private final SalesRepository salesRepository;
    private final FacilityRepository facilityRepository;
    private final ManagementRepository managementRepository;

    @GetMapping({"/", "/main"})
    public String index(HttpServletRequest request, Model model) {
        request.setAttribute("activeMenu", "main");
        
        // 최근 PC 요청건 5건
        try {
            var pcRequirePage = pcInfoRequireService.listAll(0, 5);
            model.addAttribute("pcRequireList", pcRequirePage.getContent());
        } catch (Exception e) {
            model.addAttribute("pcRequireList", List.of());
        }
        
        // 최근 세일즈 게시글 5건 (타입 상관없이)
        try {
            var salesList = salesRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
            ).getContent();
            model.addAttribute("salesList", salesList);
        } catch (Exception e) {
            model.addAttribute("salesList", List.of());
        }
        
        // 최근 facility 게시글 5건
        try {
            var facilityList = facilityRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
            ).getContent();
            model.addAttribute("facilityList", facilityList);
        } catch (Exception e) {
            model.addAttribute("facilityList", List.of());
        }
        
        // 최근 management 게시글 5건
        try {
            var managementList = managementRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
            ).getContent();
            model.addAttribute("managementList", managementList);
        } catch (Exception e) {
            model.addAttribute("managementList", List.of());
        }
        
        return "main";
    }

}
