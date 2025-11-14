package com.example.teams.auth.controller;

import com.example.teams.auth.service.AzureOAuthService;
import com.example.teams.shared.port.GraphClientPort;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Microsoft 단독 로그인 컨트롤러
 * 
 * MS 단독 로그인 (DB 연결 없이도 동작)
 * - Azure에 등록된 엔드포인트이므로 변경 불가
 * - DB 연결 없이 MS 로그인만으로 동작
 * - Access Token을 세션에 저장하여 Graph API 사용
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AzureLoginController {
    
    private final AzureOAuthService azureOAuthService;
    private final GraphClientPort graphClientPort;
    
    /**
     * OAuth Callback 처리 (MS 단독 로그인)
     * Azure에 등록된 엔드포인트 - 변경 불가
     * DB 연결 없이도 동작 가능
     * response_mode=form_post로 인해 POST 요청으로 처리
     */
    @PostMapping("/auth/microsoft/callback")
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
            // Access Token 및 Refresh Token 획득
            String[] tokens = azureOAuthService.getAccessToken(code);
            String accessToken = tokens[0];
            // String refreshToken = tokens[1];
            
            // 세션에 저장 (DB 연결 없이도 동작)
            session.setAttribute("accessToken", accessToken);
            
            // Graph Client 초기화
            graphClientPort.initializeGraphClient(accessToken);

            log.info("OAuth 인증 성공! (MS 단독 로그인 - DB 미사용)");
            redirectAttributes.addFlashAttribute("success", 
                "로그인 성공!");
            
            return "redirect:/home";
        } catch (Exception e) {
            log.error("토큰 교환 실패", e);
            redirectAttributes.addFlashAttribute("error", 
                "토큰 획득 실패: " + e.getMessage());
            return "redirect:/";
        }
    }
    
    /**
     * 계정 선택 창을 강제로 띄워 로그인 (MS 단독 로그인)
     */
    @GetMapping("/auth/microsoft/login/select-account")
    public RedirectView selectAccountLogin() {
        String url = azureOAuthService.getAuthorizationUrlWith("select_account", null);
        return new RedirectView(url);
    }

    /**
     * 로그인 창 강제 표시 (MS 단독 로그인)
     */
    @GetMapping("/auth/microsoft/login/force")
    public RedirectView forceLogin() {
        String url = azureOAuthService.getAuthorizationUrlWith("login", null);
        return new RedirectView(url);
    }
    
}

