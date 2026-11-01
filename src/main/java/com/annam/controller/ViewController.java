package com.annam.controller;

import com.annam.model.User;
import com.annam.model.UserRole;
import com.annam.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/")
    public String index(HttpSession session) {
        if (session != null) {
            Object obj = session.getAttribute(AuthService.SESSION_USER_KEY);
            if (obj instanceof User user) {
                if (user.getRole() == UserRole.ADMIN) {
                    return "redirect:/index.html";
                } else if (user.getRole() == UserRole.DONOR) {
                    return "redirect:/donor-dashboard.html";
                }
            }
        }
        return "redirect:/login.html";
    }

    @GetMapping("/login")
    public String login() {
        return "redirect:/login.html";
    }

    @GetMapping("/register")
    public String register() {
        return "redirect:/register.html";
    }
}
