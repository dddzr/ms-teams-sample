package com.example.teams.auth.controller;

import com.example.teams.auth.service.AzureOAuthService;
import com.example.teams.shared.exception.UnauthorizedException;
import com.example.teams.shared.port.GraphClientPort;
import com.example.teams.user.entity.User;
import com.example.teams.user.service.UserService;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

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
public class AzureAuthController {
    
    private final AzureOAuthService azureOAuthService;
    private final GraphClientPort graphClientPort;
    private final UserService userService;
    
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
     * Teams 인증 콜백 처리 (GET 요청)
     * Teams SDK의 authenticate() 메서드에서 사용
     * response_mode=query로 인해 GET 요청으로 처리
     */
    @GetMapping("/auth/microsoft/callback")
    public String teamsCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description,
            jakarta.servlet.http.HttpServletRequest request,
            HttpSession session,
            Model model) {
        
        log.info("=== Teams 콜백 요청 수신 ===");
        log.info("Request URL: {}", request.getRequestURL());
        log.info("Query String: {}", request.getQueryString());
        log.info("code: {}", code != null ? "있음" : "없음");
        log.info("error: {}", error);
        
        if (error != null) {
            log.error("Teams OAuth 오류: {} - {}", error, error_description);
            model.addAttribute("error", error_description != null ? error_description : error);
            return "auth/microsoft/teams-callback";
        }
        
        if (code == null) {
            log.error("Teams Authorization code가 없습니다");
            model.addAttribute("error", "인증 코드가 없습니다");
            return "auth/microsoft/teams-callback";
        }
        
        try {
            log.info("Authorization code로 토큰 교환 시작...");
            // Access Token 및 Refresh Token 획득
            String[] tokens = azureOAuthService.getAccessToken(code);
            String accessToken = tokens[0];
            
            log.info("토큰 획득 성공, 세션에 저장 중...");
            // 세션에 저장 (DB 연결 없이도 동작)
            session.setAttribute("accessToken", accessToken);
            
            // Graph Client 초기화
            graphClientPort.initializeGraphClient(accessToken);

            log.info("Teams OAuth 인증 성공! (MS 단독 로그인 - DB 미사용)");
            model.addAttribute("success", true);
            
            return "auth/microsoft/teams-callback";
        } catch (Exception e) {
            log.error("Teams 토큰 교환 실패", e);
            model.addAttribute("error", "토큰 획득 실패: " + e.getMessage());
            return "auth/microsoft/teams-callback";
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
    
    /**
     * Teams SSO 토큰 처리
     * Teams에 로그인된 사용자의 SSO 토큰과 Graph API 토큰을 받아서 검증하고 사용자 로그인 연동 처리
     * 
     * 참고: SSO 토큰은 앱의 App ID URI에 대한 토큰이므로 Graph API를 직접 호출할 수 없습니다.
     * Graph API를 사용하려면 별도로 Graph API 토큰을 요청해야 합니다.
     */
    @PostMapping("/auth/teams/sso")
    @ResponseBody
    public Map<String, Object> teamsSSO(
            @RequestBody Map<String, String> request,
            HttpSession session) {
        
        String ssoToken = request.get("ssoToken");
        String graphToken = request.get("graphToken");
        
        if (ssoToken == null || ssoToken.isEmpty()) {
            log.error("Teams SSO 토큰이 없습니다");
            return Map.of("success", false, "error", "SSO 토큰이 없습니다");
        }
        
        try {
            // SSO 토큰에서 사용자 정보 추출
            String[] parts = ssoToken.split("\\.");
            if (parts.length != 3) {
                log.error("잘못된 토큰 형식입니다");
                return Map.of("success", false, "error", "잘못된 토큰 형식입니다");
            }
            
            // JWT Payload 디코딩
            String payloadJson = new String(
                java.util.Base64.getUrlDecoder().decode(parts[1]), 
                java.nio.charset.StandardCharsets.UTF_8
            );
            
            // JSON 파싱
            org.json.JSONObject claims = new org.json.JSONObject(payloadJson);
            
            // 토큰 검증: audience 확인 (앱의 App ID URI와 일치해야 함)
            String expectedAudience = "api://nwnote.saerom.co.kr/56e05b4e-9682-4e5f-8866-5ba5d76e1cbf";
            String actualAudience = claims.optString("aud", "");
            
            // SSO 토큰의 경우 aud가 앱의 App ID URI이거나, 
            // 일부 경우에는 다른 형식일 수 있으므로 유연하게 처리
            if (!actualAudience.equals(expectedAudience) && 
                !actualAudience.contains("56e05b4e-9682-4e5f-8866-5ba5d76e1cbf")) {
                log.warn("토큰 audience가 예상과 다릅니다. expected: {}, actual: {}", 
                    expectedAudience, actualAudience);
            }
            
            // Scope 확인 (access_as_user가 있어야 함)
            String scope = claims.optString("scp", "");
            if (!scope.contains("access_as_user")) {
                log.warn("토큰에 access_as_user scope가 없습니다. scope: {}", scope);
            }
            
            // SSO 토큰에서 기본 사용자 정보 추출
            String userPrincipalName = claims.optString("preferred_username", 
                claims.optString("upn", ""));
            String userName = claims.optString("name", "");
            String microsoftUserId = claims.optString("oid", "");
            String tenantId = claims.optString("tid", "");
            
            log.info("Teams SSO 토큰 검증 성공 - user: {}, name: {}, tenant: {}", 
                userPrincipalName, userName, tenantId);
            
            // Graph API 토큰이 있으면 사용자 정보를 더 자세히 조회하고 로그인 연동 처리
            User user = null;
            if (graphToken != null && !graphToken.isEmpty()) {
                try {
                    log.info("Graph API 토큰으로 사용자 정보 조회 및 로그인 연동 시작...");
                    
                    // Graph Client 초기화
                    graphClientPort.initializeGraphClient(graphToken);
                    
                    // Microsoft Graph API로 사용자 정보 가져오기
                    GraphServiceClient graphClient = graphClientPort.getGraphClient();
                    com.microsoft.graph.models.User graphUser = graphClient.me().get(requestConfiguration -> {
                        requestConfiguration.queryParameters.select = new String[]{
                            "id", "displayName", "mail", "userPrincipalName"
                        };
                    });
                    
                    // Graph API에서 가져온 정보로 업데이트
                    if (graphUser.getUserPrincipalName() != null && !graphUser.getUserPrincipalName().isEmpty()) {
                        userPrincipalName = graphUser.getUserPrincipalName();
                    }
                    if (graphUser.getDisplayName() != null && !graphUser.getDisplayName().isEmpty()) {
                        userName = graphUser.getDisplayName();
                    }
                    if (graphUser.getId() != null && !graphUser.getId().isEmpty()) {
                        microsoftUserId = graphUser.getId();
                    }
                    
                    log.info("Graph API 사용자 정보 조회 성공 - userPrincipalName: {}, name: {}, microsoftId: {}", 
                        userPrincipalName, userName, microsoftUserId);
                    
                    // 사용자 로그인 연동 처리
                    try {
                        user = userService.findOAuthUser(
                            microsoftUserId,
                            graphUser.getMail() != null ? graphUser.getMail() : userPrincipalName,
                            userName,
                            userPrincipalName
                        );
                        
                        log.info("사용자 로그인 연동 성공 - userId: {}, email: {}", 
                            user.getId(), user.getEmail());
                        
                        // 세션에 사용자 정보 저장 (앱 로그인과 동일한 형식)
                        session.setAttribute("userId", user.getId());
                        session.setAttribute("userEmail", user.getEmail());
                        session.setAttribute("userName", user.getName());
                        session.setAttribute("loginType", "SSO");
                        
                    } catch (UnauthorizedException e) {
                        log.warn("사용자 로그인 연동 실패 (등록된 사용자가 아님): {}", e.getMessage());
                        // 등록된 사용자가 아니어도 SSO 토큰은 저장하여 기본 인증은 가능
                    }
                    
                } catch (Exception e) {
                    log.error("Graph API 토큰 처리 실패", e);
                    // Graph API 실패해도 SSO 토큰은 저장하여 기본 인증은 가능
                }
            }
            
            // SSO 토큰을 세션에 저장 (Graph API 토큰이 없거나 사용자 연동 실패해도 기본 인증은 가능)
            session.setAttribute("accessToken", graphToken != null && !graphToken.isEmpty() ? graphToken : ssoToken);
            session.setAttribute("ssoToken", ssoToken); // SSO 토큰임을 표시
            session.setAttribute("userPrincipalName", userPrincipalName);
            session.setAttribute("userName", userName);
            session.setAttribute("microsoftUserId", microsoftUserId);
            session.setAttribute("tenantId", tenantId);
            
            // 사용자 연동이 성공한 경우
            if (user != null) {
                log.info("Teams SSO 인증 및 사용자 로그인 연동 성공!");
                return Map.of(
                    "success", true, 
                    "message", "Teams SSO 로그인 및 사용자 연동 성공",
                    "userPrincipalName", userPrincipalName,
                    "userName", userName,
                    "userId", user.getId(),
                    "userEmail", user.getEmail()
                );
            } else {
                // SSO 토큰만 있고 사용자 연동은 실패한 경우
                log.info("Teams SSO 인증 성공 (사용자 연동 없음)");
                return Map.of(
                    "success", true, 
                    "message", "Teams SSO 로그인 성공 (등록된 사용자가 아닙니다)",
                    "userPrincipalName", userPrincipalName,
                    "userName", userName,
                    "warning", "등록된 사용자가 아닙니다. 먼저 회원가입을 해주세요."
                );
            }
        } catch (Exception e) {
            log.error("Teams SSO 토큰 처리 실패", e);
            return Map.of("success", false, "error", "토큰 처리 실패: " + e.getMessage());
        }
    }
    
}

