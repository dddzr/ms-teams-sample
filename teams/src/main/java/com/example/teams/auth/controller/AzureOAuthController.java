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
 */
@Controller
@RequestMapping("/auth/oauth")
@RequiredArgsConstructor
@Slf4j
public class AzureOAuthController {
    
    private final AzureOAuthService azureOAuthService;
    private final GraphClientPort graphClientPort;
    private final UserService userService;
    private final CommonAuthController commonAuthController;
    
    /**
     * OAuth 로그인 시작
     * Microsoft Entra ID로 리다이렉트
     */
    @GetMapping("/login")
    public RedirectView login() {
        String url = azureOAuthService.getOAuthAuthorizationUrl();
        log.info("OAuth 로그인 시작: {}", url);
        return new RedirectView(url);
    }
    
    /**
     * Teams 인증 콜백 처리 (GET 요청)
     * Teams SDK의 authenticate() 메서드에서 사용
     * response_mode=query로 인해 GET 요청으로 처리
     */
    @GetMapping("/callback")
    public String teamsCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description,
            jakarta.servlet.http.HttpServletRequest request,
            HttpSession session,
            Model model) {
        
            log.info("=== Teams OAuth 콜백 요청 수신 ===");
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
            // Access Token 및 Refresh Token 획득 (OAuth 설정 사용)
            String[] tokens = azureOAuthService.getAccessToken(code, true);
            String accessToken = tokens[0];
            String refreshToken = tokens[1];
            
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
                // OAuth 로그인 실패 시 MS 토큰 및 Graph Client도 제거 (OAuth 실패 = MS 로그인도 실패)
                commonAuthController.clearMsAuthentication(session, false);
                log.error("userPrincipalName 정보가 없습니다. OAuth 로그인 실패 - MS 토큰 및 Graph Client 제거");
                model.addAttribute("error", "userPrincipalName 정보가 없습니다.");
                return "auth/microsoft/teams-callback";
            }
            
            log.info("OAuth 로그인 시도: userPrincipalName={}, mail={}", userPrincipalName, graphUser.getMail());
            
            // userPrincipalName을 email로 사용하여 DB에서 사용자 찾기
            User user = userService.findByEmail(userPrincipalName)
                    .orElse(null);
            
            if (user == null) {
                // OAuth 로그인 실패 시 MS 토큰 및 Graph Client도 제거 (OAuth 실패 = MS 로그인도 실패)
                commonAuthController.clearMsAuthentication(session, false);
                log.error("등록된 사용자가 아닙니다. OAuth 로그인 실패 - MS 토큰 및 Graph Client 제거: userPrincipalName={}", userPrincipalName);
                model.addAttribute("error", "등록된 사용자가 아닙니다. 먼저 회원가입을 해주세요.");
                return "auth/microsoft/teams-callback";
            }
            
            // OAuth 정보 업데이트 (없으면 추가)
            // linkOAuth()가 업데이트된 User를 반환하므로 반환값 사용
            if (user.getMicrosoftId() == null) {
                user = userService.linkOAuth(
                    user.getId(),
                    graphUser.getId(),
                    graphUser.getUserPrincipalName()
                );
            }
            
            // Refresh Token 저장 및 마지막 로그인 시간 업데이트 (Access Token은 세션에만 저장)
            if (refreshToken != null) {
                userService.saveRefreshToken(user.getId(), refreshToken);
            } else {
                userService.updateLastLoginAt(user.getId());
            }
            
            // JPA 영속성 컨텍스트가 자동으로 업데이트를 반영하므로 첫 번째 조회한 user 객체를 그대로 사용
            // 세션에 사용자 정보 저장
            session.setAttribute("userId", user.getId());
            session.setAttribute("userEmail", user.getEmail());
            session.setAttribute("userName", user.getName());
            session.setAttribute("loginType", "OAUTH");
            session.setAttribute("accessToken", accessToken);
            
            log.info("Teams OAuth 인증 성공! 사용자 ID: {}", user.getId());
            model.addAttribute("success", true);
            
            return "auth/microsoft/teams-callback";
        } catch (UnauthorizedException e) {
            // UnauthorizedException은 명시적으로 처리 (OAuth 로그인 실패)
            commonAuthController.clearMsAuthentication(session, false);
            log.error("OAuth 로그인 실패 (UnauthorizedException): {} - MS 토큰 및 Graph Client 제거", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "auth/microsoft/teams-callback";
        } catch (Exception e) {
            // 기타 예외 처리 (OAuth 로그인 실패)
            commonAuthController.clearMsAuthentication(session, false);
            log.error("Teams OAuth 토큰 교환 실패 - MS 토큰 및 Graph Client 제거", e);
            model.addAttribute("error", "토큰 획득 실패: " + e.getMessage());
            return "auth/microsoft/teams-callback";
        }
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
            // Access Token 및 Refresh Token 획득 (OAuth 설정 사용)
            String[] tokens = azureOAuthService.getAccessToken(code, true);
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
            // linkOAuth()가 업데이트된 User를 반환하므로 반환값 사용
            if (user.getMicrosoftId() == null) {
                user = userService.linkOAuth(
                    user.getId(),
                    graphUser.getId(),
                    graphUser.getUserPrincipalName()
                );
            }
            
            // Refresh Token 저장 및 마지막 로그인 시간 업데이트 - TODO: 암호화 (Access Token은 세션에만 저장)
            if (refreshToken != null) {
                userService.saveRefreshToken(user.getId(), refreshToken);
            } else {
                // Refresh Token이 없어도 마지막 로그인 시간은 업데이트
                userService.updateLastLoginAt(user.getId());
            }
            
            // JPA 영속성 컨텍스트가 자동으로 업데이트를 반영하므로 첫 번째 조회한 user 객체를 그대로 사용
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
        String url = azureOAuthService.getAuthorizationUrlWith("select_account", null);
        return new RedirectView(url);
    }

    /**
     * 로그인 창 강제 표시
     */
    @GetMapping("/login/force")
    public RedirectView forceLogin() {
        String url = azureOAuthService.getAuthorizationUrlWith("login", null);
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
            // Access Token 획득 (OAuth 설정 사용)
            String[] tokens = azureOAuthService.getAccessToken(code, true);
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
            
            // Refresh Token 저장 (Access Token은 세션에만 저장)
            if (refreshToken != null) {
                userService.saveRefreshToken(user.getId(), refreshToken);
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

