package com.park.welstory.wooriportal.pcinfo.pcinfoRequire;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;



@Controller
@RequiredArgsConstructor
public class PcInfoRequireController {

    private final PcInfoRequireService requireService;
    @GetMapping("/pcinforequire")
    public String view(Model model, Pageable pageable,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String status,
                       HttpServletRequest request) {

        request.setAttribute("activeMenu", "facility");
        request.setAttribute("activeSubMenu", "facilitypcinforequire");

        int page = pageable.getPageNumber();
        var dtoPage = (status!=null && !status.isBlank()) ? requireService.listByStatus(status, page, size) : requireService.listAll(page, size);
        model.addAttribute("page", dtoPage);
        model.addAttribute("content", dtoPage.getContent());
        model.addAttribute("size", size);
        model.addAttribute("status", status);
        return "common/pcinforequire"; }

    @GetMapping("/pcinforequire/list")
    @ResponseBody
    public Page<PcInfoRequireDTO> listAll(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return requireService.listAll(page, size);
    }

    @PostMapping("/pcinforequire/{reNum}/reply")
    @ResponseBody
    public PcInfoRequireDTO reply(@PathVariable Long reNum, @RequestBody PcInfoRequireDTO dto) {
        return requireService.reply(reNum, dto.getReWriter(), dto.getReContent());
    }

    @PostMapping("/pcinforequire/{reNum}/status")
    @ResponseBody
    public void status(@PathVariable Long reNum, @RequestParam String status) {
        requireService.updateStatus(reNum, status);
    }



    @GetMapping("/pcinfo/require/pc/{pcInfoNum}")
    @ResponseBody
    public Page<PcInfoRequireDTO> list(@PathVariable Long pcInfoNum,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "5") int size) {
        return requireService.listByPcInfo(pcInfoNum, page, size);
    }

    @PostMapping("/pcinfo/require/add")
    @ResponseBody
    public PcInfoRequireDTO add(@RequestBody PcInfoRequireDTO dto) {
        try {
            Long pcInfoNum = dto.getPcInfo() != null ? dto.getPcInfo().getPcInfoNum() : null;
            if (pcInfoNum == null) {
                throw new IllegalArgumentException("PC 정보가 없습니다.");
            }
            return requireService.add(pcInfoNum, dto.getReWriter(), dto.getReSeat(), dto.getReContent());
        } catch (IllegalArgumentException e) {
            throw e; // 명시적 예외는 그대로 전달
        } catch (Exception e) {
            throw new RuntimeException("요청 등록 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @DeleteMapping("/pcinfo/require/{reNum}")
    @ResponseBody
    public void delete(@PathVariable Long reNum) {
        requireService.delete(reNum);
    }
}


