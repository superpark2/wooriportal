package com.park.welstory.wooriportal.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class SkinAdvice {
    @ModelAttribute
    public void addSkinToModel(HttpServletRequest request, Model model) {
        String skin = "default";
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("selectedSkin".equals(cookie.getName())) {
                    skin = cookie.getValue();
                    break;
                }
            }
        }
        model.addAttribute("skin", skin);
    }
} 