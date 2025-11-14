package com.example.teams.auth.controller;

import com.example.teams.auth.config.SamlConfig;
import com.example.teams.auth.service.SamlService;
import com.example.teams.user.entity.User;
import com.example.teams.user.service.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * SAML 2.0 인증 컨트롤러
 * 
 * 시나리오: 우리 포털이 IdP (Identity Provider)
 * - 우리 포털이 사용자 인증을 담당
 * - Microsoft Entra ID는 Relying Party (RP)로 동작
 * - 사용자가 우리 포털에서 로그인하면 Microsoft Entra ID가 우리 포털의 인증 정보를 받음
 * 
 * 엔드포인트:
 * - /auth/saml/sso - SSO 시작 (Microsoft Entra ID로부터 AuthnRequest 수신)
 * - /auth/saml/metadata - IdP 메타데이터 제공
 * - /auth/saml/login - 로그인 페이지 (SAML 인증용)
 * - /auth/saml/assert - Assertion 생성 및 전송
 */
@Controller
@RequestMapping("/auth/saml")
@RequiredArgsConstructor
@Slf4j
public class SamlController {
    
    private final SamlService samlService;
    private final SamlConfig samlConfig;
    private final UserService userService;
    
    /**
     * SSO 시작
     * Microsoft Entra ID로부터 AuthnRequest를 받아 로그인 페이지로 리다이렉트
     * 
     * 파라미터:
     * - SAMLRequest: Base64 인코딩된 AuthnRequest
     * - RelayState: 원래 요청한 리소스 정보
     */
    @GetMapping("/sso")
    public String sso(
            @RequestParam(required = false) String SAMLRequest,
            @RequestParam(required = false) String RelayState,
            HttpSession session,
            Model model) {
        
        log.info("SAML SSO 요청 수신: SAMLRequest={}, RelayState={}", SAMLRequest, RelayState);
        
        if (SAMLRequest == null) {
            log.error("SAMLRequest 파라미터가 없습니다");
            model.addAttribute("error", "SAMLRequest 파라미터가 없습니다");
            return "error";
        }
        
        // 세션에 SAMLRequest와 RelayState 저장
        session.setAttribute("samlRequest", SAMLRequest);
        session.setAttribute("relayState", RelayState);
        
        // 로그인 페이지로 리다이렉트
        return "redirect:/auth/saml/login";
    }
    
    /**
     * 로그인 페이지
     * SAML 인증을 위한 로그인 폼
     */
    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        String samlRequest = (String) session.getAttribute("samlRequest");
        if (samlRequest == null) {
            model.addAttribute("error", "SAML 요청이 없습니다");
            return "error";
        }
        
        model.addAttribute("samlRequest", samlRequest);
        return "auth/saml/login";
    }
    
    /**
     * 로그인 처리
     * 사용자 인증 후 SAML Response 생성 및 전송
     * 
     * 참고: 현재는 우리 DB의 사용자로 로그인하는 방식입니다.
     * Microsoft Entra ID의 사용자와 매칭하려면:
     * 1. Microsoft Entra ID에서 사용자 정보를 받아서
     * 2. 우리 DB에서 해당 이메일로 사용자를 찾거나 생성해야 합니다.
     * 
     * 현재 구현: 우리 포털의 사용자가 Microsoft 서비스에 접근하는 시나리오
     * - 우리 포털이 IdP
     * - Microsoft가 SP
     * - 우리 DB의 사용자로 로그인하여 Microsoft에 인증 정보 전송
     */
    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        try {
            // 사용자 인증 (우리 DB의 사용자)
            User user = userService.login(
                new com.example.teams.user.dto.LoginRequest(email, password)
            );
            
            // 세션에 사용자 정보 저장
            session.setAttribute("userId", user.getId());
            session.setAttribute("userEmail", user.getEmail());
            session.setAttribute("userName", user.getName());
            session.setAttribute("loginType", "SAML_IDP");
            
            // SAMLRequest 가져오기
            String samlRequest = (String) session.getAttribute("samlRequest");
            String relayState = (String) session.getAttribute("relayState");
            
            if (samlRequest == null) {
                redirectAttributes.addFlashAttribute("error", "SAML 요청이 없습니다");
                return "redirect:/";
            }
            
            // AuthnRequest 파싱 (간단한 버전 - 실제로는 더 복잡함)
            String inResponseTo = extractInResponseTo(samlRequest);
            
            // SAML Response 생성
            // 주의: 여기서 user.getEmail()을 NameID로 사용하므로,
            // Microsoft Entra ID의 사용자와 이메일이 일치해야 합니다.
            String samlResponse = samlService.createSamlResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                inResponseTo
            );
            
            // 세션에 SAML Response 저장
            session.setAttribute("samlResponse", samlResponse);
            session.setAttribute("relayState", relayState);
            
            log.info("SAML 로그인 성공: 사용자 ID={}, 이메일={}", user.getId(), user.getEmail());
            
            // Assertion 전송 페이지로 리다이렉트
            return "redirect:/auth/saml/assert";
            
        } catch (Exception e) {
            log.error("SAML 로그인 실패", e);
            redirectAttributes.addFlashAttribute("error", 
                "로그인 실패: " + e.getMessage());
            return "redirect:/auth/saml/login";
        }
    }
    
    /**
     * Assertion 전송
     * SAML Response를 Microsoft Entra ID로 POST 전송
     */
    @GetMapping("/assert")
    public String assertResponse(HttpSession session, Model model) {
        String samlResponse = (String) session.getAttribute("samlResponse");
        String relayState = (String) session.getAttribute("relayState");
        
        log.info("SAML Assertion 페이지 요청: samlResponse 존재={}, 길이={}, relayState={}", 
            samlResponse != null, samlResponse != null ? samlResponse.length() : 0, relayState);
        
        if (samlResponse == null || samlResponse.isEmpty()) {
            log.error("SAML Response가 없거나 비어있습니다. 세션 ID: {}", session.getId());
            model.addAttribute("error", "SAML Response가 없습니다");
            return "error";
        }
        
        // Microsoft Entra ID의 ACS URL (설정에서 가져오기)
        String acsUrl = samlConfig.getAcsUrl();
        
        log.info("SAML Assertion 전송 준비: acsUrl={}, samlResponse 길이={}, relayState={}", 
            acsUrl, samlResponse.length(), relayState);
        
        // SAML Response가 너무 짧으면 문제가 있을 수 있음
        if (samlResponse.length() < 100) {
            log.warn("SAML Response가 비정상적으로 짧습니다: 길이={}", samlResponse.length());
        }
        
        // 모델에 값 추가 (디버깅용 로그 추가)
        model.addAttribute("acsUrl", acsUrl);
        model.addAttribute("samlResponse", samlResponse);
        model.addAttribute("relayState", relayState != null ? relayState : "");
        
        log.info("모델에 값 추가 완료: acsUrl={}, samlResponse 길이={}, relayState={}", 
            acsUrl, samlResponse.length(), relayState != null ? relayState.length() : 0);
        
        // 세션 정리 (폼 제출 후에 정리하는 것이 더 안전할 수 있음)
        // 일단 주석 처리하여 디버깅
        // session.removeAttribute("samlRequest");
        // session.removeAttribute("samlResponse");
        // session.removeAttribute("relayState");
        
        return "auth/saml/assert";
    }
    
    /**
     * IdP 메타데이터 제공
     * Microsoft Entra ID에 등록할 우리 포털의 메타데이터
     */
    @GetMapping(value = "/metadata", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String metadata() {
        log.info("SAML 메타데이터 요청");
        return samlService.generateMetadata();
    }
    
    /**
     * AuthnRequest에서 InResponseTo 추출 (간단한 버전)
     * 실제로는 XML 파싱이 필요함
     */
    private String extractInResponseTo(String samlRequest) {
        // TODO: 실제 XML 파싱 구현 필요
        // 임시로 랜덤 ID 반환
        return "req_" + System.currentTimeMillis();
    }
}

