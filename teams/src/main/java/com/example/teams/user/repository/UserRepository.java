package com.example.teams.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.teams.user.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 이메일로 사용자 찾기
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Microsoft ID로 사용자 찾기
     */
    Optional<User> findByMicrosoftId(String microsoftId);
    
    /**
     * 이메일 또는 Microsoft ID로 사용자 찾기
     */
    Optional<User> findByEmailOrMicrosoftId(String email, String microsoftId);
    
    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);
    
    /**
     * Microsoft ID 존재 여부 확인
     */
    boolean existsByMicrosoftId(String microsoftId);
}

