package com.example.teams.shared.util;

import com.example.teams.shared.exception.UnauthorizedException;
import com.example.teams.shared.port.GraphClientPort;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * 인증 관련 유틸리티
 * 모든 컨트롤러에서 공통으로 사용하는 인증 체크 로직을 제공합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthUtil {
    
    private final GraphClientPort graphClientPort;
    
    // 관리자 권한으로 간주할 수 있는 scope 목록
    private static final List<String> ADMIN_SCOPES = List.of(
        "Directory.ReadWrite.All",
        "Directory.Read.All",
        "User.ReadWrite.All",
        "Group.ReadWrite.All",
        "Team.ReadWrite.All",
        "TeamMember.ReadWrite.All",
        "Application.ReadWrite.All"
    );
    
    // 관리자 역할 ID 목록 (wids - Well-known IDs)
    private static final List<String> ADMIN_ROLE_IDS = List.of(
        "62e90394-69f5-4237-9190-012177145e10", // Company Administrator (Global Administrator)
        "f2ef992c-3afb-46b9-b7cf-a126ee74c451"  // User Administrator
    );

    /**
     * 앱 로그인 + MS 로그인 모두 지원.
     * 
     * @param session HTTP 세션
     * @throws UnauthorizedException 인증되지 않은 경우
     */
    public void checkAuthentication(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        String accessToken = (String) session.getAttribute("accessToken");
        String loginType = (String) session.getAttribute("loginType");
        
        // MS 단독 로그인: Access Token만으로 인증
        if (userId == null && accessToken != null) {
            graphClientPort.initializeGraphClient(accessToken);
            return;
        }
        
        // 앱 로그인 또는 OAuth 연동 로그인: userId가 필요
        if (userId == null) {
            throw new UnauthorizedException("인증되지 않은 사용자입니다. 먼저 로그인해주세요.");
        }
        
        // OAuth 로그인인 경우 Access Token이 필요하고 Graph Client 초기화
        if ("OAUTH".equals(loginType) || "BOTH".equals(loginType)) {
            if (accessToken == null) {
                throw new UnauthorizedException("OAuth 토큰이 없습니다. 다시 로그인해주세요.");
            }
            graphClientPort.initializeGraphClient(accessToken);
        }
        // 앱 로그인만 한 경우 (OAuth 미연동)는 Graph Client 초기화 불필요
    }
    
    /**
     * 세션에서 사용자 ID를 가져옵니다.
     * 
     * @param session HTTP 세션
     * @return 사용자 ID (없으면 null)
     */
    public Long getUserId(HttpSession session) {
        return (Long) session.getAttribute("userId");
    }
    
    /**
     * 세션에서 로그인 타입을 가져옵니다.
     * 
     * @param session HTTP 세션
     * @return 로그인 타입 (APP, OAUTH, BOTH)
     */
    public String getLoginType(HttpSession session) {
        return (String) session.getAttribute("loginType");
    }
    
    /**
     * OAuth가 연동되어 있는지 확인합니다.
     * 
     * @param session HTTP 세션
     * @return OAuth 연동 여부
     */
    public boolean isOAuthLinked(HttpSession session) {
        String loginType = getLoginType(session);
        return "OAUTH".equals(loginType) || "BOTH".equals(loginType);
    }
    
    /**
     * 세션에서 Access Token을 가져옵니다.
     * 
     * @param session HTTP 세션
     * @return Access Token (없으면 null)
     */
    public String getAccessToken(HttpSession session) {
        return (String) session.getAttribute("accessToken");
    }
    
    /**
     * 세션에 Access Token을 저장합니다.
     * 
     * @param session HTTP 세션
     * @param accessToken Access Token
     */
    public void setAccessToken(HttpSession session, String accessToken) {
        session.setAttribute("accessToken", accessToken);
    }
    
    /**
     * 세션에서 Access Token을 제거합니다 (로그아웃 시 사용).
     * 
     * @param session HTTP 세션
     */
    public void removeAccessToken(HttpSession session) {
        session.removeAttribute("accessToken");
    }
    
    /**
     * 세션의 accessToken에서 관리자 권한이 있는지 확인합니다.
     * 
     * @param session HTTP 세션
     * @return 관리자 권한이 있으면 true
     */
    public boolean isAdmin(HttpSession session) {
        String accessToken = (String) session.getAttribute("accessToken");
        if (accessToken == null) {
            return false;
        }
        
        try {
            // JWT 토큰 디코딩
            String[] parts = accessToken.split("\\.");
            if (parts.length != 3) {
                log.warn("잘못된 JWT 토큰 형식");
                return false;
            }
            
            // Payload 디코딩
            String payloadJson = new String(
                Base64.getUrlDecoder().decode(parts[1]), 
                StandardCharsets.UTF_8
            );
            JSONObject claims = new JSONObject(payloadJson);
            
            // scope 확인
            // if (claims.has("scp")) {
            //     String scope = claims.getString("scp");
            //     log.debug("토큰 scope: {}", scope);
                
            //     // 관리자 scope가 포함되어 있는지 확인
            //     for (String adminScope : ADMIN_SCOPES) {
            //         if (scope.contains(adminScope)) {
            //             log.info("관리자 권한 확인: {}", adminScope);
            //             return true;
            //         }
            //     }
            // }
            
            // roles 확인 (앱 역할)
            // if (claims.has("roles")) {
            //     Object rolesObj = claims.get("roles");
            //     if (rolesObj instanceof List) {
            //         @SuppressWarnings("unchecked")
            //         List<String> roles = (List<String>) rolesObj;
            //         log.debug("토큰 roles: {}", roles);
                    
            //         // 관리자 역할이 있는지 확인
            //         for (String role : roles) {
            //             if (role.toLowerCase().contains("admin") || 
            //                 role.toLowerCase().contains("administrator")) {
            //                 log.info("관리자 역할 확인: {}", role);
            //                 return true;
            //             }
            //         }
            //     }
            // }
            
            // wids 확인 (Well-known IDs - Azure AD 역할 ID)
            if (claims.has("wids")) {
                Object widsObj = claims.get("wids");
                if (widsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> wids = (List<String>) widsObj;
                    log.debug("토큰 wids: {}", wids);
                    
                    // 관리자 역할 ID가 포함되어 있는지 확인
                    for (String wid : wids) {
                        if (ADMIN_ROLE_IDS.contains(wid)) {
                            log.info("관리자 역할 ID 확인: {}", wid);
                            return true;
                        }
                    }
                }
            }
            
            // log.debug("관리자 권한이 확인되지 않았습니다");
            return false;
            
        } catch (Exception e) {
            log.warn("관리자 권한 확인 실패: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 토큰의 scope 문자열에서 관리자 권한이 있는지 확인합니다.
     * 
     * @param scopeString scope 문자열
     * @return 관리자 권한이 있으면 true
     */
    public boolean hasAdminScope(String scopeString) {
        if (scopeString == null || scopeString.isEmpty()) {
            return false;
        }
        
        for (String adminScope : ADMIN_SCOPES) {
            if (scopeString.contains(adminScope)) {
                return true;
            }
        }
        
        return false;
    }
}

