package com.example.teams.controller;

import com.example.teams.dto.UserDto;
import com.example.teams.exception.ForbiddenException;
import com.example.teams.service.GraphClientService;
import com.example.teams.util.AuthUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 관리자 페이지 컨트롤러
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/admin")
public class AdminController {
    
    private final AuthUtil authUtil;
    private final GraphClientService graphClientService;
    
    /**
     * 관리자 권한 확인 API
     */
    @GetMapping("/check")
    @ResponseBody
    public Map<String, Object> checkAdmin(HttpSession session) {
        authUtil.checkAuthentication(session);
        
        boolean isAdmin = authUtil.isAdmin(session);
        String accessToken = (String) session.getAttribute("accessToken");
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("isAdmin", isAdmin);
        
        // 토큰에서 scope 정보 추출
        try {
            if (accessToken != null) {
                String[] parts = accessToken.split("\\.");
                if (parts.length == 3) {
                    String payloadJson = new String(
                        Base64.getUrlDecoder().decode(parts[1]), 
                        StandardCharsets.UTF_8
                    );
                    JSONObject claims = new JSONObject(payloadJson);
                    
                    if (claims.has("scp")) {
                        result.put("scopes", claims.getString("scp"));
                    }
                    if (claims.has("roles")) { // MS Token에는 이거 없다!! 대신 wids로 scope 허용 권한 있지만 실제로는 내부 DB에서 관리하는 역할 조회해야한다.
                        result.put("roles", claims.get("roles"));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("토큰 정보 추출 실패: {}", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 토큰 정보 조회 API
     */
    @GetMapping("/token-info")
    @ResponseBody
    public Map<String, Object> getTokenInfo(HttpSession session) {
        authUtil.checkAuthentication(session);
        
        // 관리자 권한 확인
        if (!authUtil.isAdmin(session)) {
            throw new ForbiddenException("관리자 권한이 필요합니다");
        }
        
        String accessToken = (String) session.getAttribute("accessToken");
        Map<String, Object> result = new java.util.HashMap<>();
        
        try {
            if (accessToken != null) {
                String[] parts = accessToken.split("\\.");
                if (parts.length == 3) {
                    // Header
                    String headerJson = new String(
                        Base64.getUrlDecoder().decode(parts[0]), 
                        StandardCharsets.UTF_8
                    );
                    result.put("header", new JSONObject(headerJson).toMap());
                    
                    // Payload
                    String payloadJson = new String(
                        Base64.getUrlDecoder().decode(parts[1]), 
                        StandardCharsets.UTF_8
                    );
                    JSONObject claims = new JSONObject(payloadJson);
                    result.put("payload", claims.toMap());
                    
                    // 민감한 정보 마스킹
                    result.put("token", accessToken.substring(0, 20) + "...");
                }
            }
        } catch (Exception e) {
            log.warn("토큰 정보 추출 실패: {}", e.getMessage());
            result.put("error", "토큰 정보 추출 실패: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 사용자 검색 API (관리자 전용)
     */
    @GetMapping("/users")
    @ResponseBody
    public List<UserDto> searchUsers(@RequestParam(required = false) String search, HttpSession session) {
        authUtil.checkAuthentication(session);
        
        // 관리자 권한 확인
        if (!authUtil.isAdmin(session)) {
            throw new ForbiddenException("관리자 권한이 필요합니다");
        }
        
        try {
            com.microsoft.graph.serviceclient.GraphServiceClient graphClient = 
                graphClientService.getGraphClient();
            
            List<UserDto> users = new ArrayList<>();
            
            if (search != null && !search.trim().isEmpty()) {
                // 사용자 검색
                var searchResults = graphClient.users().get(requestConfiguration -> {
                    requestConfiguration.queryParameters.filter = 
                        "startswith(displayName,'" + search + "') or startswith(mail,'" + search + "') or startswith(userPrincipalName,'" + search + "')";
                    requestConfiguration.queryParameters.top = 20;
                });
                
                if (searchResults != null && searchResults.getValue() != null) {
                    searchResults.getValue().forEach(user -> {
                        users.add(UserDto.builder()
                            .id(user.getId())
                            .displayName(user.getDisplayName())
                            .mail(user.getMail())
                            .userPrincipalName(user.getUserPrincipalName())
                            .jobTitle(user.getJobTitle())
                            .build());
                    });
                }
            } else {
                // 검색어가 없으면 최근 사용자 목록 (최대 20명)
                var allUsers = graphClient.users().get(requestConfiguration -> {
                    requestConfiguration.queryParameters.top = 20;
                });
                
                if (allUsers != null && allUsers.getValue() != null) {
                    allUsers.getValue().forEach(user -> {
                        users.add(UserDto.builder()
                            .id(user.getId())
                            .displayName(user.getDisplayName())
                            .mail(user.getMail())
                            .userPrincipalName(user.getUserPrincipalName())
                            .jobTitle(user.getJobTitle())
                            .build());
                    });
                }
            }
            
            return users;
        } catch (Exception e) {
            log.error("사용자 검색 실패", e);
            throw new RuntimeException("사용자 검색 실패: " + e.getMessage());
        }
    }
}

