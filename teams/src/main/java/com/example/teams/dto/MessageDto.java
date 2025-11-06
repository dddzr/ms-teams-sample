package com.example.teams.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {
    private String id;
    private String subject;
    private String body;
    private String userName;
    private String webUrl;
    private OffsetDateTime createdDateTime;
    private String messageType;
}

