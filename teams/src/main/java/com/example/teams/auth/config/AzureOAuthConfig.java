package com.example.teams.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@ConfigurationProperties(prefix = "azure")
@Getter
@Setter
public class AzureOAuthConfig {
    private String clientId;
    private String clientSecret;
    private String tenantId;
    private String redirectUri;
    private String scope;
    
    // OAuth 2.0 설정 (MS가 IdP → 내 앱 로그인 연동)
    private OAuthConfig oauth = new OAuthConfig();
    
    @Getter
    @Setter
    public static class OAuthConfig {
        private String clientId;
        private String clientSecret;
        private String tenantId;
        private String redirectUri;
        private String scope;
    }
    
    /**
     * 공통 Authorization URL 생성 메서드
     */
    private String buildAuthorizationUrl(String tenantId, String clientId, String redirectUri, String scope, String responseMode) {
        try {
            String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString());
            String encodedScope = URLEncoder.encode(scope, StandardCharsets.UTF_8.toString());
            
            return String.format(
                "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize?" +
                "client_id=%s&response_type=code&redirect_uri=%s&scope=%s&response_mode=%s",
                tenantId, clientId, encodedRedirectUri, encodedScope, responseMode
            );
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("URL 인코딩 실패", e);
        }
    }
    
    /**
     * MS 단독 로그인용 Authorization URL 생성
     */
    public String getAuthorizationUrl() {
        return buildAuthorizationUrl(tenantId, clientId, redirectUri, scope, "form_post");
    }
    
    /**
     * MS 단독 로그인용 Teams SDK Authorization URL 생성 (response_mode=query)
     */
    public String getAuthorizationUrlForTeams() {
        return buildAuthorizationUrl(tenantId, clientId, redirectUri, scope, "query");
    }
    
    /**
     * MS 단독 로그인용 Token URL
     */
    public String getTokenUrl() {
        return String.format(
            "https://login.microsoftonline.com/%s/oauth2/v2.0/token",
            tenantId
        );
    }
    
    /**
     * OAuth 2.0 (MS가 IdP → 내 앱 로그인 연동)용 Authorization URL 생성
     */
    public String getOAuthAuthorizationUrl() {
        return buildAuthorizationUrl(
            oauth.getTenantId(), 
            oauth.getClientId(), 
            oauth.getRedirectUri(), 
            oauth.getScope(), 
            "form_post"
        );
    }
    
    /**
     * OAuth 2.0 (MS가 IdP → 내 앱 로그인 연동)용 Teams SDK Authorization URL 생성
     */
    public String getOAuthAuthorizationUrlForTeams() {
        return buildAuthorizationUrl(
            oauth.getTenantId(), 
            oauth.getClientId(), 
            oauth.getRedirectUri(), 
            oauth.getScope(), 
            "query"
        );
    }
    
    /**
     * OAuth 2.0용 Token URL
     */
    public String getOAuthTokenUrl() {
        return String.format(
            "https://login.microsoftonline.com/%s/oauth2/v2.0/token",
            oauth.getTenantId()
        );
    }
}

