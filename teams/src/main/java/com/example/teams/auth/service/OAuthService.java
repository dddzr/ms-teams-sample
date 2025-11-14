package com.example.teams.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.example.teams.auth.config.OAuthConfig;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * OAuth 2.0 서비스
 * 
 * 시나리오: Microsoft Entra ID가 IdP (Identity Provider)
 * - Microsoft Entra ID가 사용자 인증을 담당
 * - 우리 앱은 Relying Party (RP)로 동작
 * - 사용자가 Microsoft 계정으로 로그인하면 우리 앱이 인증 정보를 받음
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthService {
    
    private final OAuthConfig oauthConfig;
    private final OkHttpClient httpClient = new OkHttpClient();
    
    /**
     * Authorization Code로 Access Token 교환
     * @param code Authorization Code
     * @return [accessToken, refreshToken] 배열 (refreshToken이 없으면 null)
     */
    public String[] getAccessToken(String code) {
        try {
            RequestBody formBody = new FormBody.Builder()
                .add("client_id", oauthConfig.getClientId())
                .add("client_secret", oauthConfig.getClientSecret())
                .add("code", code)
                .add("redirect_uri", oauthConfig.getRedirectUri())
                .add("grant_type", "authorization_code")
                .add("scope", oauthConfig.getScope())
                .build();
            // TODO: state 추가 및 응답에서 요청과 동일한지 검증 (CSRF 공격 방지

            Request request = new Request.Builder()
                .url(oauthConfig.getTokenUrl())
                .post(formBody)
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Token 요청 실패: {}", response.code());
                    throw new RuntimeException("Token 요청 실패: " + response.code());
                }
                
                String responseBody = response.body().string();
                JSONObject json = new JSONObject(responseBody);
                String accessToken = json.getString("access_token");
                String refreshToken = json.optString("refresh_token", null);
                
                // 토큰 응답에서 scope 확인 (디버깅용)
                if (json.has("scope")) {
                    String scope = json.getString("scope");
                    log.info("토큰에 포함된 scope: {}", scope);
                } else {
                    log.warn("토큰 응답에 scope 정보가 없습니다");
                }
                
                // JWT claims에서 테넌트/계정 유형 로깅
                try {
                    String[] parts = accessToken.split("\\.");
                    if (parts.length == 3) {
                        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                        JSONObject claims = new JSONObject(payloadJson);
                        String tid = claims.optString("tid", "unknown");
                        String iss = claims.optString("iss", "unknown");
                        String aud = claims.optString("aud", "unknown");
                        String preferredUsername = claims.optString("preferred_username", claims.optString("upn", ""));
                        String tenantType = iss.contains("consumers") ? "personal (MSA)" : (iss.contains("organizations") || iss.contains("common")) ? "work/school (Org)" : "unknown";
                        log.info("JWT claims: tid={}, iss={}, aud={}, preferred_username={}", tid, iss, aud, preferredUsername);
                        log.info("계정 유형 추정: {}", tenantType);
                    }
                } catch (Exception ex) {
                    log.warn("JWT claims 디코딩 실패: {}", ex.getMessage());
                }

                log.info("Access Token 획득 성공");
                return new String[]{accessToken, refreshToken};
            }
        } catch (IOException e) {
            log.error("Access Token 획득 실패", e);
            throw new RuntimeException("Access Token 획득 실패", e);
        }
    }
    
    /**
     * Authorization URL 생성
     */
    public String getAuthorizationUrl() {
        return oauthConfig.getAuthorizationUrl();
    }

    /**
     * 추가 파라미터(prompt, login_hint)를 포함한 Authorization URL 생성
     */
    public String getAuthorizationUrlWith(String prompt, String loginHint) {
        String base = oauthConfig.getAuthorizationUrl();
        StringBuilder sb = new StringBuilder(base);
        boolean hasQuery = base.contains("?");
        String sep = hasQuery ? "&" : "?";
        try {
            if (prompt != null && !prompt.isBlank()) {
                sb.append(sep).append("prompt=").append(URLEncoder.encode(prompt, StandardCharsets.UTF_8.toString()));
                sep = "&";
            }
            if (loginHint != null && !loginHint.isBlank()) {
                sb.append(sep).append("login_hint=").append(URLEncoder.encode(loginHint, StandardCharsets.UTF_8.toString()));
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Authorization URL 생성 실패", e);
        }
        return sb.toString();
    }
}

