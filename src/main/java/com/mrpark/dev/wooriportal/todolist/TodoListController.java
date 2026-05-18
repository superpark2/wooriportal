package com.mrpark.dev.wooriportal.todolist;

import com.mrpark.dev.wooriportal.board.BoardTitleService;
import com.mrpark.dev.wooriportal.config.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class TodoListController {

    private final TodoListService todoListService;
    private final BoardTitleService boardTitleService;

    // ── 기존 (변경 없음) ─────────────────────────────────────────────────────

    @GetMapping("/{group}/todolist")
    public String todoList(@PathVariable String group, Model model,
                           HttpServletRequest request, Pageable pageable,
                           @AuthenticationPrincipal CustomUserDetails user) {
        String title = boardTitleService.boardTitle(group);
        model.addAttribute("title", title);
        model.addAttribute("subTitle", "To Do List");
        request.setAttribute("activeSubMenu", "personaltodo");
        if (title.isEmpty()) return "redirect:/";
        model.addAttribute("todolist", todoListService.todoList(user.getMemberNum(), pageable));
        return "common/todolist";
    }

    @PostMapping("/{group}/todolist")
    public String addToDoList(@PathVariable String group, TodoListDTO todoListDTO,
                              @AuthenticationPrincipal CustomUserDetails user) {
        todoListService.addToDoList(todoListDTO, user.getMemberNum());
        return "redirect:/" + group + "/todolist";
    }

    @PutMapping("/{group}/todolist/{todoNum}")
    @ResponseBody
    public void updateToDoList(@PathVariable String group, @PathVariable Long todoNum,
                               TodoListDTO todoListDTO) {
        todoListService.updateToDoList(todoNum, todoListDTO);
    }

    @DeleteMapping("/{group}/todolist/{todoNum}")
    @ResponseBody
    public void deleteToDoList(@PathVariable String group, @PathVariable Long todoNum) {
        todoListService.deleteToDoList(todoNum);
    }

    @PatchMapping("/{group}/todolist/{todoNum}/toggle")
    @ResponseBody
    public void toggleToDoStatus(@PathVariable String group, @PathVariable Long todoNum) {
        todoListService.toggleToDoStatus(todoNum);
    }

    // ── 대시보드 위젯 전용 ────────────────────────────────────────────────────

    /** POST /api/todolist  →  저장 즉시 todoNum 포함 DTO 반환 */
    @PostMapping("/api/todolist")
    @ResponseBody
    public ResponseEntity<TodoListDTO> apiAdd(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails user) {
        String title = body.getOrDefault("todoTitle", "").trim();
        if (title.isEmpty()) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(todoListService.addAndReturn(title, user.getMemberNum()));
    }
}