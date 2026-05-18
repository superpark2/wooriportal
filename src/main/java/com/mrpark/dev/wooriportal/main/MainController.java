package com.mrpark.dev.wooriportal.main;

import com.mrpark.dev.wooriportal.board.BoardDTO;
import com.mrpark.dev.wooriportal.board.BoardService;
import com.mrpark.dev.wooriportal.config.security.CustomUserDetails;
import com.mrpark.dev.wooriportal.pcinfo.require.PcInfoRequireService;
import com.mrpark.dev.wooriportal.todolist.TodoListRepository;
import com.mrpark.dev.wooriportal.todolist.TodoListDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final BoardService boardService;
    private final PcInfoRequireService pcInfoRequireService;
    private final TodoListRepository todoListRepository;  // 레포지토리 직접 — 단순 목록 조회라 충분
    private final ModelMapper modelMapper = new ModelMapper();

    @GetMapping({"", "/", "/main"})
    public String index(HttpServletRequest request, Model model,
                        @AuthenticationPrincipal CustomUserDetails user) {
        request.setAttribute("activeMenu", "main");

        Long memberNum = user != null ? user.getMemberNum() : null;
        if (user != null) model.addAttribute("memberName", user.getMemberName());

        // ── PC 요청 최근 5건
        try {
            var pcList = pcInfoRequireService.listAll(0, 5).getContent();
            model.addAttribute("pcRequireList", pcList);
            model.addAttribute("pcPendingCount", pcList.stream().filter(d -> "미완료".equals(d.getReStatus())).count());
            model.addAttribute("pcDoneCount",    pcList.stream().filter(d -> "완료".equals(d.getReStatus())).count());
        } catch (Exception e) {
            model.addAttribute("pcRequireList",  List.of());
            model.addAttribute("pcPendingCount", 0L);
            model.addAttribute("pcDoneCount",    0L);
        }

        // ── 게시판 최근 5건 + 전체 수 (BoardService → BoardDTO, setCategory 한글 변환 적용)
        var sortDesc = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        for (String group : List.of("sales", "facility", "management")) {
            try {
                var page = boardService.list(group, "board", memberNum, sortDesc);
                model.addAttribute(group + "List",       page.getContent());
                model.addAttribute(group + "TotalCount", page.getTotalElements());
            } catch (Exception e) {
                model.addAttribute(group + "List",       List.of());
                model.addAttribute(group + "TotalCount", 0L);
            }
        }

        // ── 나의 할 일 (레포지토리 직접 → ModelMapper로 DTO 변환)
        try {
            if (memberNum != null) {
                var todoEntities = todoListRepository.findByMember_MemberNum(
                        memberNum,
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
                ).getContent();
                List<TodoListDTO> todoList = todoEntities.stream()
                        .map(e -> modelMapper.map(e, TodoListDTO.class))
                        .toList();
                model.addAttribute("todoList", todoList);
            } else {
                model.addAttribute("todoList", List.of());
            }
        } catch (Exception e) {
            model.addAttribute("todoList", List.of());
        }

        return "main";
    }
}