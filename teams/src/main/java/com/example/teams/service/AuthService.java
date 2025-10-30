package com.example.teams.service;

import com.example.teams.config.AzureAdConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final AzureAdConfig azureAdConfig;
    private final OkHttpClient httpClient = new OkHttpClient();
    
    /**
     * Authorization Code로 Access Token 교환
     */
    public String getAccessToken(String code) {
        try {
            RequestBody formBody = new FormBody.Builder()
                .add("client_id", azureAdConfig.getClientId())
                .add("client_secret", azureAdConfig.getClientSecret())
                .add("code", code)
                .add("redirect_uri", azureAdConfig.getRedirectUri())
                .add("grant_type", "authorization_code")
                .add("scope", azureAdConfig.getScope())
                .build();
            
            Request request = new Request.Builder()
                .url(azureAdConfig.getTokenUrl())
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
                
                log.info("Access Token 획득 성공");
                return accessToken;
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
        return azureAdConfig.getAuthorizationUrl();
    }
}

