package com.example.teams.auth.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.config.InitializationException;
import org.springframework.stereotype.Component;

/**
 * SAML 2.0 초기화 클래스
 * 
 * OpenSAML 라이브러리를 초기화합니다.
 * 애플리케이션 시작 시 자동으로 실행됩니다.
 */
@Component
@Slf4j
public class SamlInitializer {
    
    @PostConstruct
    public void initialize() {
        try {
            InitializationService.initialize();
            log.info("OpenSAML 초기화 완료");
        } catch (InitializationException e) {
            log.error("OpenSAML 초기화 실패", e);
            throw new RuntimeException("OpenSAML 초기화 실패", e);
        }
    }
}

