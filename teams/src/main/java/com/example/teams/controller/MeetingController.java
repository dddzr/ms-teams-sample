package com.example.teams.controller;

import com.example.teams.dto.MeetingCreateRequest;
import com.example.teams.dto.MeetingDto;
import com.example.teams.service.MeetingService;
import com.example.teams.util.AuthUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Microsoft Teams Online Meetings 관련 API 컨트롤러
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/me/onlineMeetings")
public class MeetingController {
    
    private final MeetingService meetingService;
    private final AuthUtil authUtil;
    
    @GetMapping
    @ResponseBody
    public List<MeetingDto> getMyMeetings(HttpSession session) {
        authUtil.checkAuthentication(session);
        return meetingService.getMyMeetings();
    }
    
    @PostMapping
    @ResponseBody
    public MeetingDto createMeeting(@RequestBody MeetingCreateRequest request, HttpSession session) {
        authUtil.checkAuthentication(session);
        return meetingService.createMeeting(request);
    }
}

