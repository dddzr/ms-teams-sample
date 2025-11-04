package com.example.teams.service;

import com.example.teams.dto.MeetingCreateRequest;
import com.example.teams.dto.MeetingDto;
import com.example.teams.util.GraphApiErrorHandler;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Microsoft Teams Online Meetings 관련 API를 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingService {
    
    private final GraphClientService graphClientService;
    private final GraphApiErrorHandler errorHandler;
    
    /**
     * 내 온라인 미팅 조회
     */
    public List<MeetingDto> getMyMeetings() {
        try {
            GraphServiceClient graphClient = graphClientService.getGraphClient();
            var meetings = graphClient.me().onlineMeetings().get(requestConfiguration -> {
                requestConfiguration.queryParameters.filter = "startDateTime ge " + 
                    OffsetDateTime.now().toString();
            });
            
            List<MeetingDto> meetingList = new ArrayList<>();
            if (meetings != null && meetings.getValue() != null) {
                meetings.getValue().forEach(meeting -> {
                    String joinUrl = "";
                    // OnlineMeeting에는 joinUrl 메서드가 없을 수 있으므로 joinWebUrl만 사용
                    meetingList.add(MeetingDto.builder()
                        .id(meeting.getId())
                        .subject(meeting.getSubject())
                        .startDateTime(meeting.getStartDateTime())
                        .endDateTime(meeting.getEndDateTime())
                        .joinUrl(joinUrl)
                        .joinWebUrl(meeting.getJoinWebUrl())
                        .build());
                });
            }
            
            log.info("미팅 조회 완료: {} 개", meetingList.size());
            return meetingList;
        } catch (Exception e) {
            errorHandler.handle(e, "미팅 조회");
            return new ArrayList<>(); // 도달하지 않음
        }
    }
    
    /**
     * 온라인 미팅 생성
     */
    public MeetingDto createMeeting(MeetingCreateRequest request) {
        try {
            GraphServiceClient graphClient = graphClientService.getGraphClient();
            com.microsoft.graph.models.OnlineMeeting meeting = new com.microsoft.graph.models.OnlineMeeting();
            meeting.setSubject(request.getSubject());
            meeting.setStartDateTime(request.getStartDateTime());
            meeting.setEndDateTime(request.getEndDateTime());
            
            var createdMeeting = graphClient.me().onlineMeetings().post(meeting);
            
            String joinUrl = "";
            // OnlineMeeting에는 joinUrl 메서드가 없을 수 있으므로 joinWebUrl만 사용
            return MeetingDto.builder()
                .id(createdMeeting.getId())
                .subject(createdMeeting.getSubject())
                .startDateTime(createdMeeting.getStartDateTime())
                .endDateTime(createdMeeting.getEndDateTime())
                .joinUrl(joinUrl)
                .joinWebUrl(createdMeeting.getJoinWebUrl())
                .build();
        } catch (Exception e) {
            errorHandler.handle(e, "미팅 생성");
            return null; // 도달하지 않음
        }
    }
}

