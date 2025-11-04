package com.example.teams.controller;

import com.example.teams.service.GraphClientService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class DashboardController {
    
    private final GraphClientService graphClientService;
    
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model, 
                          RedirectAttributes redirectAttributes) {
        String accessToken = (String) session.getAttribute("accessToken");
        
        if (accessToken == null) {
            redirectAttributes.addFlashAttribute("error", 
                "먼저 로그인해주세요");
            return "redirect:/";
        }
        
        try {
            // Graph Client 초기화
            graphClientService.initializeGraphClient(accessToken);
            
            return "dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "대시보드 로드 실패: " + e.getMessage());
            return "redirect:/";
        }
    }
}

