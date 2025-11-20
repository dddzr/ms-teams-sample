package com.example.teams.user.controller;

import com.example.teams.shared.util.AuthUtil;
import com.example.teams.user.entity.User;
import com.example.teams.user.service.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 앱(DB) 사용자 정보 조회 컨트롤러
 * MS Graph API와 무관한 앱 자체 사용자 정보를 조회합니다.
 */
@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserService userService;
    private final AuthUtil authUtil;
    
    /**
     * 앱(DB) 사용자 정보 조회
     * 세션의 userId를 기반으로 DB에서 사용자 정보를 조회합니다.
     * MS 단독 로그인인 경우 userId가 없을 수 있으므로 404를 반환합니다.
     */
    @GetMapping("/me")
    @ResponseBody
    public Map<String, Object> getAppUser(HttpSession session) {
        Long userId = authUtil.getUserId(session);
        if (userId == null) {
            // MS 단독 로그인 또는 세션이 전달되지 않은 경우 404 반환
            // 프론트엔드에서 response.ok로 체크하므로 정상적으로 처리됨
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "앱 로그인이 필요합니다.");
        }
        
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("email", user.getEmail());
        userInfo.put("name", user.getName());
        userInfo.put("loginType", user.getLoginType() != null ? user.getLoginType().name() : null);
        userInfo.put("microsoftId", user.getMicrosoftId());
        userInfo.put("userPrincipalName", user.getUserPrincipalName());
        userInfo.put("oauthLinked", user.isOAuthLinked());
        userInfo.put("createdAt", user.getCreatedAt() != null ? 
                user.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        userInfo.put("lastLoginAt", user.getLastLoginAt() != null ? 
                user.getLastLoginAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        userInfo.put("oauthLinkedAt", user.getOauthLinkedAt() != null ? 
                user.getOauthLinkedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        
        // 비밀번호는 제외
        
        return userInfo;
    }
}

