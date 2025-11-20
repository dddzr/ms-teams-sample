package com.example.teams.auth.controller;

import com.example.teams.shared.port.GraphClientPort;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 공통 인증 컨트롤러
 * 
 * 모든 인증 방식에 공통으로 사용되는 인증 관련 기능을 처리합니다.
 * - 로그아웃 (모든 인증 방식 공통)
 */
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class CommonAuthController {
    
    private final GraphClientPort graphClientPort;
    
    /**
     * MS 인증 정보 초기화 (세션 및 Graph Client)
     * OAuth 로그인 실패 시 또는 로그아웃 시 사용
     * 
     * @param session HTTP 세션
     * @param invalidateSession true면 세션 전체 무효화, false면 accessToken만 제거
     */
    public void clearMsAuthentication(HttpSession session, boolean invalidateSession) {
        // Graph Client 리셋
        if (graphClientPort.isGraphClientInitialized()) {
            graphClientPort.reset();
        }
        
        if (invalidateSession) {
            // 세션 전체 무효화 (로그아웃)
            session.invalidate();
        } else {
            // accessToken만 제거 (OAuth 로그인 실패)
            session.removeAttribute("accessToken");
        }
    }
    
    /**
     * 로그아웃 (공통)
     * 모든 인증 방식(앱, MS, OAuth, SAML)에 공통으로 사용되는 로그아웃 기능
     */
    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        clearMsAuthentication(session, true);
        
        log.info("로그아웃 완료");
        redirectAttributes.addFlashAttribute("success", "로그아웃 되었습니다");
        return "redirect:/";
    }
}

