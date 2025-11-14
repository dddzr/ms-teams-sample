package com.example.teams.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth 2.0 설정
 * 
 * 시나리오: Microsoft Entra ID가 IdP (Identity Provider)
 * - Microsoft Entra ID가 사용자 인증을 담당
 * - 사용자가 Microsoft 계정으로 로그인하면 우리 앱이 인증 정보를 받음
 */
@Configuration
@ConfigurationProperties(prefix = "azure.oauth")
@Getter
@Setter
public class OAuthConfig {
    private String clientId;
    private String clientSecret;
    private String tenantId;
    private String redirectUri;
    private String scope;
    
    /**
     * OAuth 2.0 Authorization URL 생성
     */
    public String getAuthorizationUrl() {
        try {
            String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString());
            String encodedScope = URLEncoder.encode(scope, StandardCharsets.UTF_8.toString());
            
            return String.format(
                "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize?" +
                "client_id=%s&response_type=code&redirect_uri=%s&scope=%s&response_mode=form_post",
                tenantId, clientId, encodedRedirectUri, encodedScope
            );
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("URL 인코딩 실패", e);
        }
    }
    
    /**
     * OAuth 2.0 Token URL
     */
    public String getTokenUrl() {
        return String.format(
            "https://login.microsoftonline.com/%s/oauth2/v2.0/token",
            tenantId
        );
    }
}

