package com.example.teams.util;

import com.example.teams.exception.UnauthorizedException;
import com.example.teams.service.GraphClientService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 인증 관련 유틸리티
 * 모든 컨트롤러에서 공통으로 사용하는 인증 체크 로직을 제공합니다.
 */
@Component
@RequiredArgsConstructor
public class AuthUtil {
    
    private final GraphClientService graphClientService;
    
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
}

