package com.example.teams.auth.controller;

import com.example.teams.auth.service.AuthService;
import com.example.teams.shared.port.GraphClientPort;
import com.example.teams.user.dto.LoginRequest;
import com.example.teams.user.dto.RegisterRequest;
import com.example.teams.user.entity.User;
import com.example.teams.user.service.UserService;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 앱 단독 로그인 컨트롤러
 * 
 * 앱 자체 로그인, 회원가입, OAuth 연동 기능을 처리합니다.
 */
@Controller
@RequestMapping("/auth/app")
@RequiredArgsConstructor
@Slf4j
public class AppLoginController {
    
    private final AuthService authService;
    private final GraphClientPort graphClientPort;
    private final UserService userService;
    
    /**
     * 앱 자체 로그인 페이지
     */
    @GetMapping("/login")
    public String appLoginPage() {
        return "auth/app/login";
    }
    
    /**
     * 앱 자체 로그인 처리
     */
    @PostMapping("/login")
    public String appLogin(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        try {
            LoginRequest request = new LoginRequest(email, password);
            User user = userService.login(request);
            
            // 세션에 사용자 정보 저장
            session.setAttribute("userId", user.getId());
            session.setAttribute("userEmail", user.getEmail());
            session.setAttribute("userName", user.getName());
            session.setAttribute("loginType", "APP");
            
            // OAuth가 연동되어 있으면 Access Token도 세션에 저장
            if (user.isOAuthLinked() && user.getAccessToken() != null) {
                session.setAttribute("accessToken", user.getAccessToken());
                graphClientPort.initializeGraphClient(user.getAccessToken());
            }
            
            log.info("앱 로그인 성공: {}", user.getEmail());
            redirectAttributes.addFlashAttribute("success", "로그인 성공!");
            return "redirect:/home";
        } catch (Exception e) {
            log.error("앱 로그인 실패", e);
            redirectAttributes.addFlashAttribute("error", 
                "로그인 실패: " + e.getMessage());
            return "redirect:/";
        }
    }
    
    /**
     * 회원가입 페이지
     */
    @GetMapping("/register")
    public String registerPage() {
        return "auth/app/register";
    }
    
    /**
     * 회원가입 처리
     */
    @PostMapping("/register")
    public String register(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String name,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        try {
            RegisterRequest request = new RegisterRequest(email, password, name);
            User user = userService.register(request);
            
            // 자동 로그인
            session.setAttribute("userId", user.getId());
            session.setAttribute("userEmail", user.getEmail());
            session.setAttribute("userName", user.getName());
            session.setAttribute("loginType", "APP");
            
            log.info("회원가입 성공: {}", user.getEmail());
            redirectAttributes.addFlashAttribute("success", "회원가입 및 로그인 성공!");
            return "redirect:/home";
        } catch (Exception e) {
            log.error("회원가입 실패", e);
            redirectAttributes.addFlashAttribute("error", 
                "회원가입 실패: " + e.getMessage());
            return "redirect:/";
        }
    }
    
    /**
     * OAuth 연동 (기존 앱 계정에 OAuth 추가)
     */
    @GetMapping("/oauth/link")
    public String linkOAuth(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", 
                "먼저 앱 로그인을 해주세요.");
            return "redirect:/";
        }
        
        if (error != null) {
            redirectAttributes.addFlashAttribute("error", 
                "OAuth 연동 실패: " + error);
            return "redirect:/home";
        }
        
        if (code == null) {
            redirectAttributes.addFlashAttribute("error", 
                "인증 코드가 없습니다");
            return "redirect:/home";
        }
        
        try {
            // Access Token 획득
            String[] tokens = authService.getAccessToken(code);
            String accessToken = tokens[0];
            String refreshToken = tokens[1];
            
            // Graph Client 초기화
            graphClientPort.initializeGraphClient(accessToken);
            
            // Microsoft Graph API로 사용자 정보 가져오기
            GraphServiceClient graphClient = graphClientPort.getGraphClient();
            com.microsoft.graph.models.User graphUser = graphClient.me().get(requestConfiguration -> {
                requestConfiguration.queryParameters.select = new String[]{
                    "id", "userPrincipalName"
                };
            });
            
            // OAuth 연동
            User user = userService.linkOAuth(
                userId,
                graphUser.getId(),
                graphUser.getUserPrincipalName()
            );
            
            // Access Token 저장
            if (refreshToken != null) {
                userService.saveAccessToken(user.getId(), accessToken, refreshToken);
            }
            
            // 세션에 Access Token 저장
            session.setAttribute("accessToken", accessToken);
            session.setAttribute("loginType", "BOTH");
            
            log.info("OAuth 연동 성공: 사용자 ID {}", userId);
            redirectAttributes.addFlashAttribute("success", 
                "Microsoft 계정 연동이 완료되었습니다!");
            return "redirect:/home";
        } catch (Exception e) {
            log.error("OAuth 연동 실패", e);
            redirectAttributes.addFlashAttribute("error", 
                "OAuth 연동 실패: " + e.getMessage());
            return "redirect:/home";
        }
    }
}

