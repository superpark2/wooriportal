package com.park.welstory.wooriportal.common.memo;

import com.park.welstory.wooriportal.common.board.BoardTitleService;
import com.park.welstory.wooriportal.config.Security.CustomUserDetails;
import com.park.welstory.wooriportal.member.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Controller
public class MemoController {
    private final MemoService memoService;
    private final MemberService memberService;
    private final BoardTitleService boardTitleService;

    // 리스트
    @GetMapping("{group}/memo")
    public String list(HttpServletRequest request, Model model,
                       @PathVariable String group,
                       @AuthenticationPrincipal CustomUserDetails user) {
        String title = boardTitleService.boardTitle(group);
        model.addAttribute("title", title);
        model.addAttribute("subTitle", "메모");
        model.addAttribute("user", user);
        if(title.isEmpty()) {
            return "redirect:/";
        }

        model.addAttribute("memoList", new java.util.ArrayList<>());
        model.addAttribute("group", group);
        request.setAttribute("activeMenu", group);
        request.setAttribute("activeSubMenu", group + "memo");
        return "common/memo";
    }

    @PostMapping("{group}/memo")
    public void write(MemoDTO memoDTO,
                      @PathVariable String group) {
        memoDTO.setDivisionGroup(group);
        memoService.addMemo(memoDTO);
    }


    @PostMapping("{group}/memo/add")
    @ResponseBody
    public String addMemo(@PathVariable String group, @RequestParam String memoContent, @AuthenticationPrincipal CustomUserDetails user) {
        MemoDTO memoDTO = new MemoDTO();
        memoDTO.setDivisionGroup(group);
        memoDTO.setMemoContent(memoContent);
        memoDTO.setMember(memberService.getMember(user.getMemberNum()));
        memoService.addMemo(memoDTO);
        return "success";
    }

    @GetMapping("{group}/memo/load")
    @ResponseBody
    public List<MemoDTO> loadMoreMemos(@PathVariable String group, Pageable pageable, @AuthenticationPrincipal CustomUserDetails user) {
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
            Sort.by("createdAt").descending());
            Long memberNum = user.getMemberNum();
        return memoService.list(group, sortedPageable, memberNum);
    }

    @DeleteMapping("{group}/memo/delete/{memoNum}")
    @ResponseBody
    public String deleteMemo(@PathVariable String group, @PathVariable Long memoNum) {
        memoService.deleteMemo(memoNum);
        return "success";
    }

}
