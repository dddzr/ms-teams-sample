package com.example.teams.ms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Microsoft Graph API 에러 응답 DTO
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphErrorResponse {
    
    @JsonProperty("error")
    private ErrorDetail error;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorDetail {
        @JsonProperty("code")
        private String code;
        
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("innerError")
        private InnerError innerError;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InnerError {
        @JsonProperty("date")
        private String date;
        
        @JsonProperty("request-id")
        private String requestId;
        
        @JsonProperty("client-request-id")
        private String clientRequestId;
    }
}

