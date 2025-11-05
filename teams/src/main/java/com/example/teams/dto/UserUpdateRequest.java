package com.example.teams.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자 정보 수정 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    /**
     * 표시 이름
     */
    private String displayName;
    
    /**
     * 직책
     */
    private String jobTitle;
    
    /**
     * 부서
     */
    private String department;
    
    /**
     * 사무실 위치
     */
    private String officeLocation;
    
    /**
     * 비즈니스 전화번호
     */
    private String businessPhone;
}

