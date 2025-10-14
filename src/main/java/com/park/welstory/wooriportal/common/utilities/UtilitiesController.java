package com.park.welstory.wooriportal.common.utilities;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UtilitiesController {

    @GetMapping("/weather")
    public String weather() {

        return "common/utilities/weather";
    }

    @GetMapping("/calculator")
    public String calculator() {
        return "common/utilities/calculator";
    }

    @GetMapping("/formatter")
    public String formatter() {
        return "common/utilities/formatter";
    }




}
