package com.example.teams.shared.port;

import com.microsoft.graph.serviceclient.GraphServiceClient;

/**
 * Graph Client 서비스 포트 인터페이스
 * 
 * 헥사고날 아키텍처의 포트(Port) 역할을 하는 인터페이스입니다.
 * auth 모듈과 ms 모듈 간의 의존성을 역전시켜 결합도를 낮춥니다.
 * 
 * 구현체는 ms 모듈에 위치합니다.
 */
public interface GraphClientPort {
    
    /**
     * Graph Client 초기화 여부 확인
     * 
     * @return 초기화되어 있으면 true
     */
    boolean isGraphClientInitialized();
    
    /**
     * Graph Client 가져오기
     * 
     * @return GraphServiceClient 인스턴스
     * @throws RuntimeException Graph Client가 초기화되지 않은 경우
     */
    GraphServiceClient getGraphClient();
    
    /**
     * Access Token으로 Graph Client 초기화
     * 같은 토큰으로 이미 초기화되어 있으면 스킵합니다.
     * 
     * @param accessToken Microsoft Graph API Access Token
     */
    void initializeGraphClient(String accessToken);
    
    /**
     * Graph Client 초기화 상태 초기화 (테스트용 또는 로그아웃 시)
     */
    void reset();
}

