package com.example.teams.shared.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.teams.ms.exception.GraphApiException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 핸들러
 * @RestController에서만 발생하는 예외를 일관된 형식으로 처리합니다.
 * 
 * @RestController 메서드: JSON 응답 반환
 * @Controller 메서드: 예외는 컨트롤러의 catch 블록에서 직접 처리 (이 핸들러는 처리하지 않음)
 */
@RestControllerAdvice(annotations = RestController.class)
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * 인증 실패 예외 처리 (401)
     * @RestController 메서드에서만 처리
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedException(UnauthorizedException e) {
        log.error("인증 실패: {}", e.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.UNAUTHORIZED.value());
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("message", e.getMessage());
        errorResponse.put("path", "/api");
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    /**
     * 권한 없음 예외 처리 (403)
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbiddenException(ForbiddenException e) {
        log.error("접근 거부: {}", e.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.FORBIDDEN.value());
        errorResponse.put("error", "Forbidden");
        errorResponse.put("message", e.getMessage());
        errorResponse.put("path", "/api");
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    /**
     * Graph API 예외 처리
     */
    @ExceptionHandler(GraphApiException.class)
    public ResponseEntity<Map<String, Object>> handleGraphApiException(GraphApiException e) {
        log.error("Graph API 호출 실패: {} (상태 코드: {})", e.getMessage(), e.getStatusCode());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", e.getStatusCode());
        errorResponse.put("error", "Graph API Error");
        errorResponse.put("message", e.getMessage());
        if (e.getErrorCode() != null) {
            errorResponse.put("errorCode", e.getErrorCode());
        }
        errorResponse.put("path", "/api");
        
        HttpStatus httpStatus = switch (e.getStatusCode()) {
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 402 -> HttpStatus.PAYMENT_REQUIRED;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        
        return ResponseEntity.status(httpStatus).body(errorResponse);
    }
    
    /**
     * 일반 런타임 예외 처리 (500)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        log.error("런타임 예외 발생: {}", e.getMessage(), e);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.put("error", "Internal Server Error");
        errorResponse.put("message", e.getMessage());
        errorResponse.put("path", "/api");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * 모든 예외 처리 (최종 안전망)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("예외 발생: {}", e.getMessage(), e);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.put("error", "Internal Server Error");
        errorResponse.put("message", "알 수 없는 오류가 발생했습니다.");
        errorResponse.put("path", "/api");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}

