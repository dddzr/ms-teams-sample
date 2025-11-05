package com.example.teams.util;

import com.example.teams.exception.UnauthorizedException;
import com.example.teams.service.GraphClientService;
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
    
    private final GraphClientService graphClientService;
    
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
    
    /**
     * 세션에서 Access Token을 확인하고 Graph Client를 초기화합니다.
     * 
     * @param session HTTP 세션
     * @throws UnauthorizedException 토큰이 없을 경우
     */
    public void checkAuthentication(HttpSession session) {
        String accessToken = (String) session.getAttribute("accessToken");
        if (accessToken == null) {
            throw new UnauthorizedException("인증되지 않은 사용자입니다. 먼저 로그인해주세요.");
        }
        graphClientService.initializeGraphClient(accessToken);
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
            if (claims.has("scp")) {
                String scope = claims.getString("scp");
                log.debug("토큰 scope: {}", scope);
                
                // 관리자 scope가 포함되어 있는지 확인
                for (String adminScope : ADMIN_SCOPES) {
                    if (scope.contains(adminScope)) {
                        log.info("관리자 권한 확인: {}", adminScope);
                        return true;
                    }
                }
            }
            
            // roles 확인 (앱 역할)
            if (claims.has("roles")) {
                Object rolesObj = claims.get("roles");
                if (rolesObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> roles = (List<String>) rolesObj;
                    log.debug("토큰 roles: {}", roles);
                    
                    // 관리자 역할이 있는지 확인
                    for (String role : roles) {
                        if (role.toLowerCase().contains("admin") || 
                            role.toLowerCase().contains("administrator")) {
                            log.info("관리자 역할 확인: {}", role);
                            return true;
                        }
                    }
                }
            }
            
            // wids 확인 (그룹 ID 기반 역할)
            if (claims.has("wids")) {
                log.debug("wids (그룹 ID) 확인됨");
                // wids가 있으면 관리자 그룹일 수 있음
                // 실제로는 Azure AD에서 관리자 그룹 ID를 확인해야 함
            }
            
            log.debug("관리자 권한이 확인되지 않았습니다");
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

