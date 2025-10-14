package com.park.welstory.wooriportal.common.todolist;

import com.park.welstory.wooriportal.common.board.BoardTitleService;
import com.park.welstory.wooriportal.config.Security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class TodoListController {

    private final TodoListService todoListService;
    private final BoardTitleService boardTitleService;

    @GetMapping("/{group}/todolist")
    public String todoList (@PathVariable String group, Model model, HttpServletRequest request, Pageable pageable,
                            @AuthenticationPrincipal CustomUserDetails user) {

        String title = boardTitleService.boardTitle(group);
        model.addAttribute("title", title);
        model.addAttribute("subTitle", "To Do List");
        request.setAttribute("activeSubMenu", "personaltodo");
        if(title.isEmpty()) {
            return "redirect:/";
        }
        Page<TodoListDTO> dto = todoListService.todoList(user.getMemberNum(), pageable);
        model.addAttribute("todolist", dto);

        return "common/todolist";
    }

    @PostMapping("/{group}/todolist")
    public String addToDoList (@PathVariable String group, TodoListDTO todoListDTO, @AuthenticationPrincipal CustomUserDetails user) {
        todoListService.addToDoList(todoListDTO, user.getMemberNum());
        return "redirect:/" + group + "/todolist";
    }

    @PutMapping("/{group}/todolist/{todoNum}")
    @ResponseBody
    public void updateToDoList (@PathVariable String group, @PathVariable Long todoNum, TodoListDTO todoListDTO) {
        todoListService.updateToDoList(todoNum, todoListDTO);
    }

    @DeleteMapping("/{group}/todolist/{todoNum}")
    @ResponseBody
    public void deleteToDoList (@PathVariable String group, @PathVariable Long todoNum) {
        todoListService.deleteToDoList(todoNum);
    }

    @PatchMapping("/{group}/todolist/{todoNum}/toggle")
    @ResponseBody
    public void toggleToDoStatus (@PathVariable String group, @PathVariable Long todoNum) {
        todoListService.toggleToDoStatus(todoNum);
    }

}
