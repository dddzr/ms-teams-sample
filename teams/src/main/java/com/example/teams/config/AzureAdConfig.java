package com.example.teams.config;

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
public class AzureAdConfig {
    private String clientId;
    private String clientSecret;
    private String tenantId;
    private String redirectUri;
    private String scope;
    
    public String getAuthorizationUrl() {
        try {
            String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString());
            String encodedScope = URLEncoder.encode(scope, StandardCharsets.UTF_8.toString());
            
            return String.format(
                "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize?" +
                "client_id=%s&response_type=code&redirect_uri=%s&scope=%s&response_mode=query",
                tenantId, clientId, encodedRedirectUri, encodedScope
            );
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("URL 인코딩 실패", e);
        }
    }
    
    public String getTokenUrl() {
        return String.format(
            "https://login.microsoftonline.com/%s/oauth2/v2.0/token",
            tenantId
        );
    }
}

