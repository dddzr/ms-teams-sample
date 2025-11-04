package com.example.teams.util;

import com.example.teams.exception.ForbiddenException;
import com.example.teams.exception.GraphApiException;
import com.example.teams.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Microsoft Graph API 에러 처리 유틸리티
 * ODataError와 ApiException을 일관된 예외로 변환합니다.
 */
@Component
@Slf4j
public class GraphApiErrorHandler {
    
    /**
     * ODataError를 적절한 예외로 변환합니다.
     * 
     * @param e ODataError
     * @param operation 작업 설명 (예: "일정 조회", "Teams 조회")
     * @throws UnauthorizedException 401 에러
     * @throws ForbiddenException 403 에러
     * @throws GraphApiException 기타 Graph API 에러
     */
    public void handleODataError(
            com.microsoft.graph.models.odataerrors.ODataError e,
            String operation) {
        
        if (e.getError() == null) {
            throw new GraphApiException(
                operation + " 실패: " + e.getMessage(),
                500,
                e
            );
        }
        
        String errorCode = e.getError().getCode();
        String errorMessage = e.getError().getMessage();
        Integer statusCode = e.getResponseStatusCode();
        
        log.error("{} 실패 - OData 에러: 코드={}, 메시지={}, 상태={}", 
            operation, errorCode, errorMessage, statusCode);
        
        if (statusCode == null) {
            throw new GraphApiException(
                operation + " 실패: " + errorMessage,
                500,
                errorCode,
                e
            );
        }
        
        switch (statusCode) {
            case 401 -> {
                log.error("인증 실패 (401): 토큰이 유효하지 않거나 필요한 권한이 없습니다.");
                throw new UnauthorizedException(
                    "인증 실패 (401): " + errorMessage,
                    e
                );
            }
            case 403 -> {
                log.error("접근 거부 (403): 권한은 있지만 리소스에 접근할 수 없습니다.");
                // Teams 미프로비저닝 체크
                if (errorMessage != null && (
                    errorMessage.contains("provisioned") ||
                    errorMessage.contains("subscription") ||
                    "Forbidden".equals(errorCode)
                )) {
                    log.warn("Teams가 테넌트에 프로비저닝되지 않았거나 유효한 Office365 구독이 없습니다.");
                    throw new ForbiddenException(
                        "Teams 미프로비저닝: " + errorMessage,
                        e
                    );
                }
                throw new ForbiddenException(
                    "접근 거부 (403): " + errorMessage,
                    e
                );
            }
            case 402 -> {
                log.error("결제 필요 (402): API 사용을 위한 구독이 필요합니다.");
                throw new GraphApiException(
                    "결제 필요 (402): " + errorMessage,
                    402,
                    errorCode,
                    e
                );
            }
            default -> throw new GraphApiException(
                operation + " 실패: " + errorMessage,
                statusCode,
                errorCode,
                e
            );
        }
    }
    
    /**
     * ApiException을 적절한 예외로 변환합니다.
     * 
     * @param e ApiException
     * @param operation 작업 설명
     * @throws UnauthorizedException 401 에러
     * @throws ForbiddenException 403 에러
     * @throws GraphApiException 기타 Graph API 에러
     */
    public void handleApiException(
            com.microsoft.kiota.ApiException e,
            String operation) {
        
        int statusCode = e.getResponseStatusCode();
        String message = e.getMessage();
        
        log.error("{} 실패 - API 예외: 상태 코드={}, 메시지={}", 
            operation, statusCode, message, e);
        
        switch (statusCode) {
            case 401 -> throw new UnauthorizedException(
                "인증 실패 (401): 토큰이 유효하지 않거나 필요한 권한이 없습니다.",
                e
            );
            case 403 -> throw new ForbiddenException(
                "접근 거부 (403): 권한은 있지만 리소스에 접근할 수 없습니다. 라이선스를 확인하세요.",
                e
            );
            case 402 -> throw new GraphApiException(
                "결제 필요 (402): API 사용을 위한 구독이 필요합니다.",
                402,
                e
            );
            default -> throw new GraphApiException(
                operation + " 실패 (상태 코드: " + statusCode + "): " + message,
                statusCode,
                e
            );
        }
    }
    
    /**
     * 일반 예외를 GraphApiException으로 변환합니다.
     * 
     * @param e 일반 예외
     * @param operation 작업 설명
     * @throws GraphApiException 변환된 예외
     */
    public void handleException(Exception e, String operation) {
        log.error("{} 실패 - 일반 에러: {}", operation, e.getMessage(), e);
        throw new GraphApiException(
            operation + " 실패: " + e.getMessage(),
            500,
            e
        );
    }
    
    /**
     * 모든 Graph API 관련 예외를 통합 처리합니다.
     * 내부에서 예외 타입을 구분하여 적절한 메서드를 호출합니다.
     * 
     * @param e 예외 (ODataError, ApiException, 또는 일반 Exception)
     * @param operation 작업 설명
     * @throws UnauthorizedException 401 에러
     * @throws ForbiddenException 403 에러
     * @throws GraphApiException 기타 Graph API 에러
     */
    public void handle(Exception e, String operation) {
        if (e instanceof com.microsoft.graph.models.odataerrors.ODataError) {
            handleODataError((com.microsoft.graph.models.odataerrors.ODataError) e, operation);
        } else if (e instanceof com.microsoft.kiota.ApiException) {
            handleApiException((com.microsoft.kiota.ApiException) e, operation);
        } else {
            handleException(e, operation);
        }
    }
}

