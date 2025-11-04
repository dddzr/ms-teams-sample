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
public class MeetingCreateRequest {
    private String subject;
    private OffsetDateTime startDateTime;
    private OffsetDateTime endDateTime;
}

