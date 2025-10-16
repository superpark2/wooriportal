package com.park.welstory.wooriportal.board.board;

import com.park.welstory.wooriportal.config.Security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@Log4j2
public class BoardController {

    private final BoardService boardService;
    private final BoardTitleService boardTitleService;

    // 게시판
    @GetMapping("{group}/{category}/list")
    public String list(HttpServletRequest request, Model model,
                       @PathVariable String group,
                       @PathVariable String category,
                       @AuthenticationPrincipal CustomUserDetails user,
                       @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String title = boardTitleService.boardTitle(group);
        model.addAttribute("title", title);
        String subTitle = boardTitleService.boardSubTitle(category);
        model.addAttribute("subTitle", subTitle);
        if(title.isEmpty() || subTitle.isEmpty()) {
            return "redirect:/";
        }
        Long memberNum = user.getMemberNum();
        Page<BoardDTO> dto = boardService.list(group, category, memberNum, pageable);
        model.addAttribute("boardList", dto);
        model.addAttribute("group", group);
        model.addAttribute("category", category);
        request.setAttribute("activeMenu", group);
        request.setAttribute("activeSubMenu", group + category);
        return "common/board/list";
    }

    @GetMapping("{group}/{category}/write")
    public String write(Model model, HttpServletRequest request,
                        @PathVariable String group,
                        @PathVariable String category) {
        model.addAttribute("group", group);
        model.addAttribute("category", category);
        String title = boardTitleService.boardTitle(group);
        model.addAttribute("title", title);
        String subTitle = boardTitleService.boardSubTitle(category);

        if(title.isEmpty() || subTitle.isEmpty()) {
            return "redirect:/";
        }
        model.addAttribute("subTitle", subTitle + " 작성");
        request.setAttribute("activeMenu", group);
        request.setAttribute("activeSubMenu", group + category);
        model.addAttribute("board", new BoardDTO());
        return "common/board/form";
    }

    @PostMapping("{group}/{category}/write")
    public String write(BoardDTO boardDTO,
                        @PathVariable String group,
                        @PathVariable String category,
                        @AuthenticationPrincipal CustomUserDetails user) {

        boardDTO.setGroup(group);
        boardDTO.setCategory(category);
        boardDTO.getMember().setMemberNum(user.getMemberNum());
        boardService.addBoard(boardDTO);
        return "redirect:/" + group + "/" + category + "/list";
    };

    @GetMapping("{group}/{category}/modify/{num}")
    public String modify(Model model, HttpServletRequest request,
                         @PathVariable String group,
                         @PathVariable String category,
                         @PathVariable Long num) {
        model.addAttribute("group", group);
        model.addAttribute("category", category);
        String title = boardTitleService.boardTitle(group);
        model.addAttribute("title", title);
        String subTitle = boardTitleService.boardSubTitle(category);
        model.addAttribute("subTitle", subTitle + " 수정");

        if(title.isEmpty() || subTitle.isEmpty()) {
            return "redirect:/";
        }

        BoardDTO dto = boardService.detailBoard(group, category, num);
        model.addAttribute("board", dto);

        request.setAttribute("activeMenu", group);
        request.setAttribute("activeSubMenu", group + category);
        return "common/board/form";
    }

    @GetMapping("{group}/{category}/detail/{num}")
    public String detail(Model model, HttpServletRequest request,
                         @PathVariable String group,
                         @PathVariable String category,
                         @PathVariable Long num,
                         @AuthenticationPrincipal CustomUserDetails user) {

        model.addAttribute("group", group);
        model.addAttribute("category", category);
        model.addAttribute("num", num);
        String title = boardTitleService.boardTitle(group);
        model.addAttribute("title", title);
        String subTitle = boardTitleService.boardSubTitle(category);
        model.addAttribute("subTitle", subTitle + "상세보기");

        if(title.isEmpty() || subTitle.isEmpty()) {
            return "redirect:/";
        }
        BoardDTO dto = boardService.detailBoard(group, category, num);
        boolean isWriter = dto.getMember() != null
                && dto.getMember().getMemberNum().equals(user.getMemberNum());
        dto.setWriter(isWriter);
        model.addAttribute("board", dto);

        request.setAttribute("activeMenu", group);
        request.setAttribute("activeSubMenu", group + category);
            return "common/board/detail";
        }

        @PostMapping("{group}/{category}/delete/{num}")
        public String delete(@PathVariable String group,
                @PathVariable String category,
                @PathVariable Long num,
                             @RequestParam Long writerNum,
                             @AuthenticationPrincipal CustomUserDetails user) {
            if (!user.getMemberNum().equals(writerNum)) {
                return "redirect:/";
            }
            boardService.deleteBoard(group, num);
            return "redirect:/" + group + "/" + category + "/list";
        }


    }
