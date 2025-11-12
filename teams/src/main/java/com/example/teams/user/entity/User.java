package com.example.teams.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 앱 사용자 엔티티
 * 앱 자체 로그인과 OAuth 로그인 모두를 지원합니다.
 */
@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "email"),
    @UniqueConstraint(columnNames = "microsoftId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 이메일 (앱 로그인용)
     */
    @Column(nullable = false, unique = true)
    private String email;
    
    /**
     * 비밀번호 (BCrypt로 암호화)
     */
    private String password;
    
    /**
     * 사용자 이름
     */
    @Column(nullable = false)
    private String name;
    
    /**
     * Microsoft OAuth ID (OAuth 로그인 시 저장)
     */
    @Column(unique = true)
    private String microsoftId;
    
    /**
     * Microsoft 사용자 Principal Name
     */
    private String userPrincipalName;
    
    /**
     * 로그인 타입: APP (앱 자체 로그인), OAUTH (OAuth 로그인), BOTH (둘 다 가능)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LoginType loginType = LoginType.APP;
    
    /**
     * OAuth Access Token (세션 대신 DB에 저장하려는 경우)
     * JWT 토큰은 최대 8000자까지 가능하므로 TEXT 타입 사용
     */
    @Column(columnDefinition = "TEXT")
    private String accessToken;
    
    /**
     * OAuth Refresh Token
     * Refresh Token도 충분한 길이를 위해 TEXT 타입 사용
     */
    @Column(columnDefinition = "TEXT")
    private String refreshToken;
    
    /**
     * 계정 생성일
     */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    /**
     * 마지막 로그인 시간
     */
    private LocalDateTime lastLoginAt;
    
    /**
     * OAuth 연동일
     */
    private LocalDateTime oauthLinkedAt;
    
    /**
     * 로그인 타입 열거형
     */
    public enum LoginType {
        APP,      // 앱 자체 로그인만
        OAUTH,    // OAuth 로그인만
        BOTH      // 둘 다 가능
    }
    
    /**
     * OAuth 연동 여부 확인
     */
    public boolean isOAuthLinked() {
        return microsoftId != null && !microsoftId.isEmpty();
    }
    
    /**
     * 앱 로그인 가능 여부 확인
     */
    public boolean canLoginWithApp() {
        return password != null && !password.isEmpty() && 
               (loginType == LoginType.APP || loginType == LoginType.BOTH);
    }
    
    /**
     * OAuth 로그인 가능 여부 확인
     */
    public boolean canLoginWithOAuth() {
        return isOAuthLinked() && 
               (loginType == LoginType.OAUTH || loginType == LoginType.BOTH);
    }
}

