package com.example.teams.controller;

import com.example.teams.service.AuthService;
import com.example.teams.service.GraphClientService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    private final GraphClientService graphClientService;
    
    /**
     * OAuth Callback 처리
     */
    @GetMapping("/callback")
    public String callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        if (error != null) {
            log.error("OAuth 오류: {} - {}", error, error_description);
            redirectAttributes.addFlashAttribute("error", 
                "인증 실패: " + error_description);
            return "redirect:/";
        }
        
        if (code == null) {
            log.error("Authorization code가 없습니다");
            redirectAttributes.addFlashAttribute("error", 
                "인증 코드가 없습니다");
            return "redirect:/";
        }
        
        try {
            // Access Token 획득
            String accessToken = authService.getAccessToken(code);
            
            // 세션에 저장
            session.setAttribute("accessToken", accessToken);
            
            // Graph Client 초기화
            graphClientService.initializeGraphClient(accessToken);
            
            log.info("인증 성공!");
            redirectAttributes.addFlashAttribute("success", 
                "로그인 성공!");
            
            return "redirect:/dashboard";
        } catch (Exception e) {
            log.error("토큰 교환 실패", e);
            redirectAttributes.addFlashAttribute("error", 
                "토큰 획득 실패: " + e.getMessage());
            return "redirect:/";
        }
    }
    
    /**
     * 계정 선택 창을 강제로 띄워 로그인
     */
    @GetMapping("/login/select-account")
    public RedirectView selectAccountLogin() {
        String url = authService.getAuthorizationUrlWith("select_account", null);
        return new RedirectView(url);
    }

    /**
     * 로그인 창 강제 표시
     */
    @GetMapping("/login/force")
    public RedirectView forceLogin() {
        String url = authService.getAuthorizationUrlWith("login", null);
        return new RedirectView(url);
    }

    /**
     * 입력한 계정으로 로그인 힌트를 전달하여 로그인
     */
    @GetMapping("/login/with-hint")
    public RedirectView loginWithHint(@RequestParam("login_hint") String loginHint) {
        String url = authService.getAuthorizationUrlWith("select_account", loginHint);
        return new RedirectView(url);
    }
    /**
     * 로그아웃
     */
    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("success", "로그아웃 되었습니다");
        return "redirect:/";
    }
}

