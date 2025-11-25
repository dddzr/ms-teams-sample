package com.example.teams.ms.service;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.OnBehalfOfCredential;
import com.azure.identity.OnBehalfOfCredentialBuilder;
import com.example.teams.auth.config.AzureOAuthConfig;
import com.example.teams.shared.port.GraphClientPort;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

/**
 * GraphServiceClient 초기화 및 관리를 담당하는 구현체
 * GraphClientPort 인터페이스의 구현체입니다.
 * 모든 서비스가 이 서비스를 통해 GraphServiceClient를 공유합니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GraphClientService implements GraphClientPort {
    
    private final AzureOAuthConfig azureOAuthConfig;
    
    private GraphServiceClient graphClient;
    private String currentAccessToken;
    private String currentSsoToken; // OBO 방식 사용 시 SSO 토큰 추적
    
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
    @Override
    public void initializeGraphClient(String accessToken) {
        try {
            // 같은 토큰으로 이미 초기화되어 있으면 스킵
            if (graphClient != null && accessToken.equals(currentAccessToken)) {
                log.debug("이미 같은 토큰으로 Graph Client가 초기화되어 있습니다");
                return;
            }
            
            // Access Token을 사용하는 커스텀 TokenCredential 생성
            // *obo 방식은 OnBehalfOfCredential 사용
            final String token = accessToken;
            TokenCredential tokenCredential = new TokenCredential() {
                @Override
                public Mono<AccessToken> getToken(TokenRequestContext request) {
                    // 1시간 후 만료 설정 (실제로는 토큰의 만료 시간을 사용해야 함)
                    OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(1);
                    return Mono.just(new AccessToken(token, expiresAt));
                }
            };
            
            // GraphServiceClient 생성
            graphClient = new GraphServiceClient(tokenCredential);
            
            log.info("Graph Client 초기화 완료");
            
            currentAccessToken = accessToken;
                
        } catch (Exception e) {
            log.error("Graph Client 초기화 실패", e);
            throw new RuntimeException("Graph Client 초기화 실패", e);
        }
    }
    
    /**
     * SSO 토큰으로 Graph Client 초기화 (OBO 방식)
     * OnBehalfOfCredential을 사용하여 자동으로 토큰 교환 및 갱신
     * 
     * @param ssoToken Teams SSO 토큰
     */
    @Override
    public void initializeGraphClientWithSSO(String ssoToken) {
        try {
            // 같은 SSO 토큰으로 이미 초기화되어 있으면 스킵
            if (graphClient != null && ssoToken.equals(currentSsoToken)) {
                log.debug("이미 같은 SSO 토큰으로 Graph Client가 초기화되어 있습니다");
                return;
            }
            
            // OAuth 설정 사용
            var oauth = azureOAuthConfig.getOauth();
            String clientId = oauth.getClientId();
            String clientSecret = oauth.getClientSecret();
            String tenantId = oauth.getTenantId();
            
            // OnBehalfOfCredential 생성 (OBO Flow 핵심)
            OnBehalfOfCredential oboCredential = new OnBehalfOfCredentialBuilder()
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .tenantId(tenantId)
                    .userAssertion(ssoToken)  // Teams에서 받은 사용자 JWT
                    .build();
            
            // GraphServiceClient 생성 (자동 토큰 교환 및 갱신)
            graphClient = new GraphServiceClient(oboCredential);
            
            log.info("Graph Client 초기화 완료 (OBO 방식 - 자동 토큰 갱신)");
            
            currentSsoToken = ssoToken;
            currentAccessToken = null; // OBO 방식에서는 직접 관리하지 않음
                
        } catch (Exception e) {
            log.error("Graph Client 초기화 실패 (OBO 방식)", e);
            throw new RuntimeException("Graph Client 초기화 실패 (OBO 방식)", e);
        }
    }
    
    /**
     * Graph Client 초기화 상태 초기화 (테스트용 또는 로그아웃 시)
     */
    public void reset() {
        graphClient = null;
        currentAccessToken = null;
        currentSsoToken = null;
        log.info("Graph Client 초기화 상태 리셋");
    }
    
    
}

