package com.example.teams.service;

import com.example.teams.dto.EventCreateRequest;
import com.example.teams.dto.EventDto;
import com.example.teams.util.GraphApiErrorHandler;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Microsoft Outlook Calendar (Events) 관련 API를 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarService {
    
    private final GraphClientService graphClientService;
    private final GraphApiErrorHandler errorHandler;
    
    /**
     * 내 일정 조회
     */
    public List<EventDto> getMyEvents() {
        try {
            log.info("일정 조회 시작...");
            GraphServiceClient graphClient = graphClientService.getGraphClient();
            // me().events() 사용 (Microsoft Graph API 표준 엔드포인트)
            var events = graphClient.me().events().get(requestConfiguration -> {
                requestConfiguration.queryParameters.top = 50;
                requestConfiguration.queryParameters.select = new String[]{
                    "id", "subject", "body", "start", "end", "location", "isAllDay"
                };
            });
            
            List<EventDto> eventList = new ArrayList<>();
            if (events != null && events.getValue() != null) {
                events.getValue().forEach(event -> {
                    String body = event.getBody() != null && event.getBody().getContent() != null ?
                        event.getBody().getContent() : "";
                    
                    OffsetDateTime startDateTime = null;
                    if (event.getStart() != null && event.getStart().getDateTime() != null) {
                        try {
                            startDateTime = OffsetDateTime.parse(event.getStart().getDateTime());
                        } catch (Exception e) {
                            log.warn("일정 시작 시간 파싱 실패: {}", event.getStart().getDateTime());
                        }
                    }
                    OffsetDateTime endDateTime = null;
                    if (event.getEnd() != null && event.getEnd().getDateTime() != null) {
                        try {
                            endDateTime = OffsetDateTime.parse(event.getEnd().getDateTime());
                        } catch (Exception e) {
                            log.warn("일정 종료 시간 파싱 실패: {}", event.getEnd().getDateTime());
                        }
                    }
                    
                    eventList.add(EventDto.builder()
                        .id(event.getId())
                        .subject(event.getSubject())
                        .body(body)
                        .start(startDateTime)
                        .end(endDateTime)
                        .location(event.getLocation() != null ? event.getLocation().getDisplayName() : null)
                        .isAllDay(event.getIsAllDay())
                        .build());
                });
            }
            
            log.info("일정 조회 완료: {} 개", eventList.size());
            return eventList;
        } catch (Exception e) {
            errorHandler.handle(e, "일정 조회");
            return new ArrayList<>(); // 도달하지 않음
        }
    }
    
    /**
     * 일정 생성
     */
    public EventDto createEvent(EventCreateRequest request) {
        try {
            GraphServiceClient graphClient = graphClientService.getGraphClient();
            com.microsoft.graph.models.Event event = new com.microsoft.graph.models.Event();
            event.setSubject(request.getSubject());
            
            com.microsoft.graph.models.ItemBody body = new com.microsoft.graph.models.ItemBody();
            body.setContent(request.getBody());
            event.setBody(body);
            
            com.microsoft.graph.models.DateTimeTimeZone start = new com.microsoft.graph.models.DateTimeTimeZone();
            start.setDateTime(request.getStart().toString());
            start.setTimeZone("Asia/Seoul");
            event.setStart(start);
            
            com.microsoft.graph.models.DateTimeTimeZone end = new com.microsoft.graph.models.DateTimeTimeZone();
            end.setDateTime(request.getEnd().toString());
            end.setTimeZone("Asia/Seoul");
            event.setEnd(end);
            
            if (request.getLocation() != null && !request.getLocation().isEmpty()) {
                com.microsoft.graph.models.Location location = new com.microsoft.graph.models.Location();
                location.setDisplayName(request.getLocation());
                event.setLocation(location);
            }
            
            var createdEvent = graphClient.me().events().post(event);
            
            String bodyContent = createdEvent.getBody() != null && createdEvent.getBody().getContent() != null ?
                createdEvent.getBody().getContent() : "";
            
            OffsetDateTime startDateTime = null;
            if (createdEvent.getStart() != null && createdEvent.getStart().getDateTime() != null) {
                try {
                    startDateTime = OffsetDateTime.parse(createdEvent.getStart().getDateTime());
                } catch (Exception e) {
                    log.warn("일정 시작 시간 파싱 실패: {}", createdEvent.getStart().getDateTime());
                }
            }
            OffsetDateTime endDateTime = null;
            if (createdEvent.getEnd() != null && createdEvent.getEnd().getDateTime() != null) {
                try {
                    endDateTime = OffsetDateTime.parse(createdEvent.getEnd().getDateTime());
                } catch (Exception e) {
                    log.warn("일정 종료 시간 파싱 실패: {}", createdEvent.getEnd().getDateTime());
                }
            }
            
            return EventDto.builder()
                .id(createdEvent.getId())
                .subject(createdEvent.getSubject())
                .body(bodyContent)
                .start(startDateTime)
                .end(endDateTime)
                .location(createdEvent.getLocation() != null ? createdEvent.getLocation().getDisplayName() : null)
                .isAllDay(createdEvent.getIsAllDay())
                .build();
        } catch (Exception e) {
            errorHandler.handle(e, "일정 생성");
            return null; // 도달하지 않음
        }
    }
}

