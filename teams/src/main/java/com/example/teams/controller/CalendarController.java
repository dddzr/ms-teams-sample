package com.example.teams.controller;

import com.example.teams.dto.EventCreateRequest;
import com.example.teams.dto.EventDto;
import com.example.teams.service.CalendarService;
import com.example.teams.util.AuthUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Microsoft Outlook Calendar (Events) 관련 API 컨트롤러
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/me/events")
public class CalendarController {
    
    private final CalendarService calendarService;
    private final AuthUtil authUtil;
    
    @GetMapping
    @ResponseBody
    public List<EventDto> getMyEvents(HttpSession session) {
        authUtil.checkAuthentication(session);
        return calendarService.getMyEvents();
    }
    
    @PostMapping
    @ResponseBody
    public EventDto createEvent(@RequestBody EventCreateRequest request, HttpSession session) {
        authUtil.checkAuthentication(session);
        return calendarService.createEvent(request);
    }
}

