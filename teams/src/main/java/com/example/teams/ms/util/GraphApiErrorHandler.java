package com.example.teams.ms.util;

import com.example.teams.ms.dto.GraphErrorResponse;
import com.example.teams.ms.exception.GraphApiException;
import com.example.teams.shared.exception.ForbiddenException;
import com.example.teams.shared.exception.UnauthorizedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Microsoft Graph API 에러 처리 유틸리티
 * ODataError와 ApiException을 일관된 예외로 변환합니다.
 */
@Component
@Slf4j
public class GraphApiErrorHandler {
    
    private final ObjectMapper objectMapper;
    
    public GraphApiErrorHandler() {
        this.objectMapper = new ObjectMapper();
    }
    
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
    }
    
    /**
     * ApiException을 적절한 예외로 변환합니다.
     * 응답 본문을 파싱하여 실제 에러 메시지를 사용합니다.
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
        
        // 응답 본문에서 에러 정보 파싱
        GraphErrorResponse errorResponse = parseErrorResponse(e);
        String errorCode = null;
        String errorMessage = null;
        
        if (errorResponse != null && errorResponse.getError() != null) {
            errorCode = errorResponse.getError().getCode();
            errorMessage = errorResponse.getError().getMessage();
        }
        
        // 파싱 실패 시 기본 메시지 사용
        if (errorMessage == null || errorMessage.isEmpty()) {
            errorMessage = e.getMessage();
        }
        
        log.error("{} 실패 - API 예외: 상태 코드={}, 에러 코드={}, 메시지={}", 
            operation, statusCode, errorCode, errorMessage, e);
        
        switch (statusCode) {
            case 401 -> throw new UnauthorizedException(
                "인증 실패 (401): " + errorMessage,
                e
            );
            case 403 -> throw new ForbiddenException(
                "접근 거부 (403): " + errorMessage,
                e
            );
            case 402 -> throw new GraphApiException(
                "결제 필요 (402): " + errorMessage,
                402,
                errorCode,
                e
            );
            default -> throw new GraphApiException(
                operation + " 실패: " + errorMessage,
                statusCode,
                errorCode,
                e
            );
        }
    }
    
    /**
     * ApiException에서 응답 본문을 파싱하여 GraphErrorResponse를 반환합니다.
     * 
     * @param e ApiException
     * @return 파싱된 에러 응답, 파싱 실패 시 null
     */
    private GraphErrorResponse parseErrorResponse(com.microsoft.kiota.ApiException e) {
        try {
            // ApiException에서 응답 본문 가져오기 시도
            String responseBody = getResponseBody(e);
            
            if (responseBody == null || responseBody.isEmpty()) {
                log.debug("응답 본문이 비어있습니다.");
                return null;
            }
            
            log.debug("응답 본문 파싱: {}", responseBody);
            return objectMapper.readValue(responseBody, GraphErrorResponse.class);
            
        } catch (Exception ex) {
            log.warn("응답 본문 파싱 실패: {}", ex.getMessage());
            return null;
        }
    }
    
    /**
     * ApiException에서 응답 본문을 가져옵니다.
     * 다양한 방법으로 시도합니다.
     * 
     * @param e ApiException
     * @return 응답 본문 문자열
     */
    private String getResponseBody(com.microsoft.kiota.ApiException e) {
        try {
            // 방법 1: getResponseBody() 메서드가 있는 경우 (리플렉션 사용)
            try {
                var method = e.getClass().getMethod("getResponseBody");
                Object responseBody = method.invoke(e);
                if (responseBody instanceof String) {
                    return (String) responseBody;
                } else if (responseBody instanceof InputStream) {
                    try (InputStream is = (InputStream) responseBody) {
                        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
            } catch (ReflectiveOperationException ignored) {
                // 메서드가 없으면 무시
            }
            
            // 방법 2: getResponse() 메서드가 있는 경우 (리플렉션 사용)
            try {
                var method = e.getClass().getMethod("getResponse");
                Object response = method.invoke(e);
                if (response instanceof InputStream) {
                    try (InputStream is = (InputStream) response) {
                        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
            } catch (ReflectiveOperationException ignored) {
                // 메서드가 없으면 무시
            }
            
            // 방법 3: getCause()를 통해 내부 예외 확인
            Throwable cause = e.getCause();
            if (cause != null && cause.getMessage() != null) {
                String causeMessage = cause.getMessage();
                // JSON 형식인지 확인
                if (causeMessage.trim().startsWith("{") && causeMessage.trim().endsWith("}")) {
                    return causeMessage;
                }
            }
            
            // 방법 4: 예외 메시지 자체가 JSON 형식일 수 있음
            String exceptionMessage = e.getMessage();
            if (exceptionMessage != null && 
                exceptionMessage.trim().startsWith("{") && 
                exceptionMessage.trim().endsWith("}")) {
                return exceptionMessage;
            }
            
        } catch (Exception ex) {
            log.debug("응답 본문 가져오기 실패: {}", ex.getMessage());
        }
        
        return null;
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

