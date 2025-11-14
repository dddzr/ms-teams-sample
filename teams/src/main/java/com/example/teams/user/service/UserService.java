package com.example.teams.user.service;

import com.example.teams.shared.exception.UnauthorizedException;
import com.example.teams.user.dto.LoginRequest;
import com.example.teams.user.dto.RegisterRequest;
import com.example.teams.user.entity.User;
import com.example.teams.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 사용자 관리 서비스
 * 앱 자체 로그인과 OAuth 연동을 처리합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    /**
     * 회원가입
     */
    @Transactional
    public User register(RegisterRequest request) {
        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }
        
        // 사용자 생성
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .loginType(User.LoginType.APP)
                .createdAt(LocalDateTime.now())
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("새 사용자 등록: {}", savedUser.getEmail());
        return savedUser;
    }
    
    /**
     * 앱 자체 로그인
     */
    @Transactional
    public User login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다."));
        
        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        
        // 앱 로그인 가능 여부 확인
        if (!user.canLoginWithApp()) {
            throw new UnauthorizedException("앱 로그인이 불가능한 계정입니다.");
        }
        
        // 마지막 로그인 시간 업데이트
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("앱 로그인 성공: {}", user.getEmail());
        return user;
    }
    
    /**
     * OAuth 로그인 시 사용자 찾기
     * Microsoft ID로 사용자를 찾고, 없으면 로그인 실패 처리합니다.
     */
    @Transactional
    public User findOAuthUser(String microsoftId, String email, String name, String userPrincipalName) {
        Optional<User> existingUser = userRepository.findByMicrosoftId(microsoftId);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // OAuth 정보 업데이트
            user.setLastLoginAt(LocalDateTime.now());
            if (user.getOauthLinkedAt() == null) {
                user.setOauthLinkedAt(LocalDateTime.now());
            }
            // 로그인 타입 업데이트 (OAuth 가능하도록)
            if (user.getLoginType() == User.LoginType.APP) {
                user.setLoginType(User.LoginType.BOTH);
            }
            return userRepository.save(user);
        }
        
        // Microsoft ID로 찾지 못한 경우, 이메일로 찾기 (기존 앱 계정과 연동)
        if (email != null && !email.isEmpty()) {
            Optional<User> userByEmail = userRepository.findByEmail(email);
            if (userByEmail.isPresent()) {
                User user = userByEmail.get();
                // 기존 앱 계정에 OAuth 정보 추가
                user.setMicrosoftId(microsoftId);
                user.setUserPrincipalName(userPrincipalName);
                user.setOauthLinkedAt(LocalDateTime.now());
                user.setLastLoginAt(LocalDateTime.now());
                // 로그인 타입을 BOTH로 변경
                if (user.getLoginType() == User.LoginType.APP) {
                    user.setLoginType(User.LoginType.BOTH);
                }
                log.info("기존 앱 계정에 OAuth 연동: {}", user.getEmail());
                return userRepository.save(user);
            }
        }
        
        // 사용자를 찾을 수 없으면 로그인 실패
        throw new UnauthorizedException("등록된 사용자가 아닙니다. 먼저 회원가입을 해주세요.");
    }
    
    /**
     * 사용자 ID로 사용자 찾기
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    /**
     * 이메일로 사용자 찾기
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    /**
     * OAuth 연동 (기존 앱 계정에 OAuth 추가)
     */
    @Transactional
    public User linkOAuth(Long userId, String microsoftId, String userPrincipalName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        if (user.isOAuthLinked()) {
            throw new IllegalArgumentException("이미 OAuth가 연동되어 있습니다.");
        }
        
        user.setMicrosoftId(microsoftId);
        user.setUserPrincipalName(userPrincipalName);
        user.setOauthLinkedAt(LocalDateTime.now());
        
        // 로그인 타입 업데이트
        if (user.getLoginType() == User.LoginType.APP) {
            user.setLoginType(User.LoginType.BOTH);
        }
        
        return userRepository.save(user);
    }
    
    /**
     * 마지막 로그인 시간 업데이트
     */
    @Transactional
    public void updateLastLoginAt(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }
    
    /**
     * Access Token 저장
     */
    @Transactional
    public void saveAccessToken(Long userId, String accessToken, String refreshToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setAccessToken(accessToken);
        user.setRefreshToken(refreshToken);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }
}

