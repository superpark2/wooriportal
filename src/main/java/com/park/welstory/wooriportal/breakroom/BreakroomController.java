package com.park.welstory.wooriportal.breakroom;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BreakroomController {

    @GetMapping("/tetris")
    public String tetris() {
        return "common/breakroom/tetris";
    }
}
