package com.example.teams.controller;

import com.example.teams.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {
    
    private final AuthService authService;
    
    @GetMapping("/")
    public String home(Model model) {
        String authUrl = authService.getAuthorizationUrl();
        model.addAttribute("authUrl", authUrl);
        return "index";
    }
}

