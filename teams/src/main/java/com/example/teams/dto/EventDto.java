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
public class EventDto {
    private String id;
    private String subject;
    private String body;
    private OffsetDateTime start;
    private OffsetDateTime end;
    private String location;
    private Boolean isAllDay;
}

