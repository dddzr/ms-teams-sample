package com.example.teams.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * SAML 2.0 설정
 * 
 * 시나리오: 우리 포털이 IdP (Identity Provider)
 * - 우리 포털이 사용자 인증을 담당
 * - Microsoft Entra ID는 Relying Party (RP)로 동작
 * - 사용자가 우리 포털에서 로그인하면 Microsoft Entra ID가 우리 포털의 인증 정보를 받음
 */
@Configuration
@ConfigurationProperties(prefix = "saml.idp")
@Getter
@Setter
public class AppSamlConfig {
    /**
     * 우리 포털의 Entity ID (IdP 식별자)
     */
    private String entityId = "https://localhost:8080/saml/idp";
    
    /**
     * SAML Assertion Consumer Service URL (Microsoft Entra ID가 응답을 받을 URL)
     */
    private String acsUrl = "https://login.microsoftonline.com/{tenant-id}/saml2";
    
    /**
     * SAML Single Logout Service URL
     */
    private String sloUrl = "https://login.microsoftonline.com/{tenant-id}/saml2";
    
    /**
     * 우리 포털의 SSO URL (사용자가 로그인하는 URL)
     */
    private String ssoUrl = "http://localhost:8080/auth/saml/sso";
    
    /**
     * 우리 포털의 메타데이터 URL
     */
    private String metadataUrl = "http://localhost:8080/auth/saml/metadata";
    
    /**
     * 인증서 파일 경로 (서명용)
     */
    private String certificatePath = "classpath:saml/certificate.pem";
    
    /**
     * 개인키 파일 경로 (서명용)
     */
    private String privateKeyPath = "classpath:saml/private-key.pem";
    
    /**
     * Microsoft Entra ID의 Entity ID (RP 식별자)
     */
    private String relyingPartyEntityId = "https://sts.windows.net/{tenant-id}/";
    
    /**
     * Assertion 유효 시간 (초)
     */
    private int assertionValiditySeconds = 300;
    
    /**
     * NameID 형식
     */
    private String nameIdFormat = "urn:oasis:names:tc:SAML:2.0:nameid-format:emailAddress";
}

