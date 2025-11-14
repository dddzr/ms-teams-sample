package com.example.teams.controller;

import com.example.teams.auth.service.AzureOAuthService;
import com.example.teams.shared.port.GraphClientPort;
import com.example.teams.shared.util.AuthUtil;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class MainController {
    
    private final GraphClientPort graphClientPort;
    private final AuthUtil authUtil;
    private final AzureOAuthService azureOAuthService;// OAuth 2.0 (MS가 IdP) 연동용

    /**
     * 메인 페이지
     * 세 가지 인증 시나리오를 테스트할 수 있는 페이지
     * 
     * 시나리오 1: MS 단독 로그인 (기존) - DB 없이도 동작, Azure 등록 엔드포인트 사용
     * 시나리오 2: OAuth 2.0 (MS가 IdP) - 내 앱 로그인 연동
     * 시나리오 3: SAML 2.0 (우리 포털이 IdP) - MS 로그인 연동
     */
    @GetMapping("/")
    public String index(Model model) {
        // 시나리오 1: MS 단독 로그인 (기존) - Azure 등록 엔드포인트 사용
        String msAuthUrl = azureOAuthService.getAuthorizationUrl();
        model.addAttribute("msAuthUrl", msAuthUrl);
        
        // 시나리오 2: OAuth 2.0 (MS가 IdP) - Authorization URL
        String oauthUrl = azureOAuthService.getAuthorizationUrl();
        model.addAttribute("oauthUrl", oauthUrl);
        
        // 시나리오 3: SAML 2.0 (우리 포털이 IdP) - 메타데이터 URL
        String samlMetadataUrl = "/auth/saml/metadata";
        model.addAttribute("samlMetadataUrl", samlMetadataUrl);
        
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
            graphClientPort.initializeGraphClient(accessToken);
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
        Long userId = (Long) session.getAttribute("userId");
        String accessToken = (String) session.getAttribute("accessToken");
        
        // 인증 확인: userId 또는 accessToken 중 하나라도 있으면 인증된 것으로 간주
        if (userId == null && accessToken == null) {
            redirectAttributes.addFlashAttribute("error", 
                "먼저 로그인해주세요");
            return "redirect:/";
        }
        
        // accessToken이 있으면 Graph Client 초기화 (MS 로그인 또는 OAuth 연동)
        if (accessToken != null) {
            try {
                graphClientPort.initializeGraphClient(accessToken);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", 
                    "페이지 로드 실패: " + e.getMessage());
                return "redirect:/";
            }
        }
        // 앱 로그인만 한 경우 (accessToken 없음)는 Graph Client 초기화 불필요
        
        return viewName;
    }
}

