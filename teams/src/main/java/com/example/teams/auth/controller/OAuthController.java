package com.example.teams.auth.controller;

import com.example.teams.auth.service.OAuthService;
import com.example.teams.shared.exception.UnauthorizedException;
import com.example.teams.shared.port.GraphClientPort;
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
import org.springframework.web.servlet.view.RedirectView;

/**
 * OAuth 2.0 인증 컨트롤러
 * 
 * 시나리오: Microsoft Entra ID가 IdP (Identity Provider)
 * - Microsoft Entra ID가 사용자 인증을 담당
 * - 우리 앱은 Relying Party (RP)로 동작
 * - 사용자가 Microsoft 계정으로 로그인하면 우리 앱이 인증 정보를 받음
 * 
 * 엔드포인트:
 * - /auth/oauth/login - OAuth 로그인 시작
 * - /auth/oauth/callback - OAuth 콜백 처리
 * - /auth/oauth/login/select-account - 계정 선택 창 강제 표시
 * - /auth/oauth/login/force - 로그인 창 강제 표시
 * - /auth/oauth/login/with-hint - 특정 계정으로 로그인 힌트 전달
 * - /auth/oauth/link - 기존 앱 계정에 OAuth 연동
 */
@Controller
@RequestMapping("/auth/oauth")
@RequiredArgsConstructor
@Slf4j
public class OAuthController {
    
    private final OAuthService oauthService;
    private final GraphClientPort graphClientPort;
    private final UserService userService;
    
    /**
     * OAuth 로그인 시작
     * Microsoft Entra ID로 리다이렉트
     */
    @GetMapping("/login")
    public RedirectView login() {
        String url = oauthService.getAuthorizationUrl();
        log.info("OAuth 로그인 시작: {}", url);
        return new RedirectView(url);
    }
    
    /**
     * OAuth Callback 처리
     * Microsoft Entra ID에서 인증 코드를 받아 토큰으로 교환
     * response_mode=form_post로 인해 POST 요청으로 처리
     */
    @PostMapping("/callback")
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
            String[] tokens = oauthService.getAccessToken(code);
            String accessToken = tokens[0];
            String refreshToken = tokens[1];
            
            // 세션에 저장 - sdk사용으로 불필요함.
            // session.setAttribute("accessToken", accessToken);

            // Graph Client 초기화
            graphClientPort.initializeGraphClient(accessToken);
            
            // Microsoft Graph API로 사용자 정보 가져오기
            GraphServiceClient graphClient = graphClientPort.getGraphClient();
            com.microsoft.graph.models.User graphUser = graphClient.me().get(requestConfiguration -> {
                requestConfiguration.queryParameters.select = new String[]{
                    "id", "displayName", "mail", "userPrincipalName"
                };
            });
            
            // userPrincipalName으로 DB의 email과 매핑
            String userPrincipalName = graphUser.getUserPrincipalName();
            if (userPrincipalName == null || userPrincipalName.isEmpty()) {
                throw new UnauthorizedException("userPrincipalName 정보가 없습니다.");
            }
            
            log.info("OAuth 로그인 시도: userPrincipalName={}, mail={}", userPrincipalName, graphUser.getMail());
            
            // userPrincipalName을 email로 사용하여 DB에서 사용자 찾기
            User user = userService.findByEmail(userPrincipalName)
                    .orElseThrow(() -> new UnauthorizedException("등록된 사용자가 아닙니다. 먼저 회원가입을 해주세요."));
            
            // OAuth 정보 업데이트 (없으면 추가)
            if (user.getMicrosoftId() == null) {
                userService.linkOAuth(
                    user.getId(),
                    graphUser.getId(),
                    graphUser.getUserPrincipalName()
                );
            }
            
            // Refresh Token 저장 및 마지막 로그인 시간 업데이트 - TODO: 암호화
            if (refreshToken != null) {
                userService.saveAccessToken(user.getId(), accessToken, refreshToken);
            } else {
                // Refresh Token이 없어도 마지막 로그인 시간은 업데이트
                userService.updateLastLoginAt(user.getId());
            }
            
            // 최신 사용자 정보 다시 조회
            user = userService.findByEmail(userPrincipalName)
                    .orElseThrow(() -> new UnauthorizedException("사용자를 찾을 수 없습니다."));
            
            // 세션에 사용자 정보 저장 (앱 로그인과 동일한 형식)
            session.setAttribute("userId", user.getId());
            session.setAttribute("userEmail", user.getEmail());
            session.setAttribute("userName", user.getName());
            session.setAttribute("loginType", "OAUTH");
            // Graph Client 초기화를 위해 accessToken도 세션에 저장
            session.setAttribute("accessToken", accessToken);
            
            log.info("OAuth 인증 성공! 사용자 ID: {}", user.getId());
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
     * 계정 선택 창을 강제로 띄워 로그인
     */
    @GetMapping("/login/select-account")
    public RedirectView selectAccountLogin() {
        String url = oauthService.getAuthorizationUrlWith("select_account", null);
        return new RedirectView(url);
    }

    /**
     * 로그인 창 강제 표시
     */
    @GetMapping("/login/force")
    public RedirectView forceLogin() {
        String url = oauthService.getAuthorizationUrlWith("login", null);
        return new RedirectView(url);
    }

    /**
     * OAuth 연동 (기존 앱 계정에 OAuth 추가)
     */
    @GetMapping("/link")
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
            String[] tokens = oauthService.getAccessToken(code);
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

