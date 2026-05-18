package com.mrpark.dev.wooriportal.utilities;

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

    @GetMapping("/test")
    public String test() {
        return "test";
    }

    @GetMapping("/student")
    public String student() {
        return "common/utilities/student";
    }

    @GetMapping("/printform")
    public String printform() {
        return "common/utilities/printform";
    }
}
