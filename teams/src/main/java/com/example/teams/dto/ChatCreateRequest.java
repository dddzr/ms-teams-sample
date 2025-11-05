package com.example.teams.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 채팅 생성 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatCreateRequest {
    /**
     * 채팅 타입: "oneOnOne" 또는 "group"
     */
    private String chatType;
    
    /**
     * 채팅에 포함할 사용자 ID 목록 (최소 2명)
     */
    private List<String> userIds;
}

