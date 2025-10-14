package com.park.welstory.wooriportal.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class breakroom {

    @GetMapping("/tetris")
    public String tetris() {

        return "common/breakroom/tetris";
    }
}
