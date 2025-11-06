package com.example.teams.controller;

import com.example.teams.service.AuthService;
import com.example.teams.service.GraphClientService;
import com.example.teams.util.AuthUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class MainController {
    
    private final GraphClientService graphClientService;
    private final AuthUtil authUtil;
    private final AuthService authService;

    @GetMapping("/")
    public String index(Model model) {
        String authUrl = authService.getAuthorizationUrl();
        model.addAttribute("authUrl", authUrl);
        return "index";
    }
    
    @GetMapping("/home")
    public String home(HttpSession session, Model model, 
                          RedirectAttributes redirectAttributes) {
        String viewName = checkAuthAndReturnView(session, redirectAttributes, "home");
        if (viewName.startsWith("redirect:")) {
            return viewName;
        }
        // 관리자 여부를 모델에 추가
        model.addAttribute("isAdmin", authUtil.isAdmin(session));
        return viewName;
    }
    
    @GetMapping("/home/profile")
    public String profile(HttpSession session, Model model, 
                          RedirectAttributes redirectAttributes) {
        String viewName = checkAuthAndReturnView(session, redirectAttributes, "profile");
        if (viewName.startsWith("redirect:")) {
            return viewName;
        }
        
        return viewName;
    }
    
    @GetMapping("/home/teams")
    public String teams(HttpSession session, RedirectAttributes redirectAttributes) {
        return checkAuthAndReturnView(session, redirectAttributes, "teams");
    }
    
    @GetMapping("/home/chats")
    public String chats(HttpSession session, RedirectAttributes redirectAttributes) {
        return checkAuthAndReturnView(session, redirectAttributes, "chats");
    }
    
    @GetMapping("/home/calendar")
    public String calendar(HttpSession session, RedirectAttributes redirectAttributes) {
        return checkAuthAndReturnView(session, redirectAttributes, "calendar");
    }
    
    @GetMapping("/home/meetings")
    public String meetings(HttpSession session, RedirectAttributes redirectAttributes) {
        return checkAuthAndReturnView(session, redirectAttributes, "meetings");
    }
    
    @GetMapping("/home/admin")
    public String admin(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String accessToken = (String) session.getAttribute("accessToken");
        
        if (accessToken == null) {
            redirectAttributes.addFlashAttribute("error", "먼저 로그인해주세요");
            return "redirect:/";
        }
        
        // 관리자 권한 확인
        if (!authUtil.isAdmin(session)) {
            redirectAttributes.addFlashAttribute("error", "관리자 권한이 필요합니다");
            return "redirect:/home";
        }
        
        try {
            graphClientService.initializeGraphClient(accessToken);
            model.addAttribute("isAdmin", true);
            return "admin";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "페이지 로드 실패: " + e.getMessage());
            return "redirect:/home";
        }
    }
    
    private String checkAuthAndReturnView(HttpSession session, 
                                         RedirectAttributes redirectAttributes, 
                                         String viewName) {
        String accessToken = (String) session.getAttribute("accessToken");
        
        if (accessToken == null) {
            redirectAttributes.addFlashAttribute("error", 
                "먼저 로그인해주세요");
            return "redirect:/";
        }
        
        try {
            // Graph Client 초기화
            graphClientService.initializeGraphClient(accessToken);
            
            return viewName;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "페이지 로드 실패: " + e.getMessage());
            return "redirect:/";
        }
    }
}

