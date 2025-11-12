package com.example.teams.shared.exception;

/**
 * 권한 없음 또는 리소스 접근 불가 예외 (403)
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
    
    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}

