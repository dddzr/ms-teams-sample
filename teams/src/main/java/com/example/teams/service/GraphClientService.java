package com.example.teams.service;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

/**
 * GraphServiceClient 초기화 및 관리를 담당하는 공통 서비스
 * 모든 서비스가 이 서비스를 통해 GraphServiceClient를 공유합니다.
 */
@Service
@Slf4j
public class GraphClientService {
    
    private GraphServiceClient graphClient;
    private String currentAccessToken;
    
    /**
     * Graph Client 초기화 여부 확인
     */
    public boolean isGraphClientInitialized() {
        return graphClient != null;
    }
    
    /**
     * Graph Client 가져오기
     */
    public GraphServiceClient getGraphClient() {
        if (graphClient == null) {
            throw new RuntimeException("Graph Client가 초기화되지 않았습니다. 먼저 initializeGraphClient()를 호출하세요.");
        }
        return graphClient;
    }
    
    /**
     * Access Token으로 Graph Client 초기화
     * 같은 토큰으로 이미 초기화되어 있으면 스킵합니다.
     */
    public void initializeGraphClient(String accessToken) {
        try {
            // 같은 토큰으로 이미 초기화되어 있으면 스킵
            if (graphClient != null && accessToken.equals(currentAccessToken)) {
                log.debug("이미 같은 토큰으로 Graph Client가 초기화되어 있습니다");
                return;
            }
            
            // Access Token을 사용하는 커스텀 TokenCredential 생성
            final String token = accessToken;
            TokenCredential tokenCredential = new TokenCredential() {
                @Override
                public Mono<AccessToken> getToken(TokenRequestContext request) {
                    // 1시간 후 만료 설정 (실제로는 토큰의 만료 시간을 사용해야 함)
                    OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(1);
                    return Mono.just(new AccessToken(token, expiresAt));
                }
            };
            
            graphClient = new GraphServiceClient(tokenCredential);
            currentAccessToken = accessToken;
                
            log.info("Graph Client 초기화 완료");
        } catch (Exception e) {
            log.error("Graph Client 초기화 실패", e);
            throw new RuntimeException("Graph Client 초기화 실패", e);
        }
    }
    
    /**
     * Graph Client 초기화 상태 초기화 (테스트용 또는 로그아웃 시)
     */
    public void reset() {
        graphClient = null;
        currentAccessToken = null;
        log.info("Graph Client 초기화 상태 리셋");
    }
}

