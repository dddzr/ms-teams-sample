package com.example.teams.ms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatDto {
    private String id;
    private String topic;
    private String chatType; // oneOnOne, group, meeting
    private OffsetDateTime createdDateTime;
    private String webUrl;
}

