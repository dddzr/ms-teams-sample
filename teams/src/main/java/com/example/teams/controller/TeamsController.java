package com.example.teams.controller;

import com.example.teams.dto.*;
import com.example.teams.service.TeamsService;
import com.example.teams.util.AuthUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Microsoft Teams 관련 API 컨트롤러
 * Teams, Channels, Channel Messages만 처리합니다.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api")
public class TeamsController {
    
    private final TeamsService teamsService;
    private final AuthUtil authUtil;
    
    // ==================== 사용자 정보 ====================
    @GetMapping("/me")
    @ResponseBody
    public UserDto getCurrentUser(HttpSession session) {
        authUtil.checkAuthentication(session);
        return teamsService.getCurrentUser();
    }
    
    // ==================== Teams & Channels ====================
    @GetMapping("/teams")
    @ResponseBody
    public List<TeamDto> getUserTeams(HttpSession session) {
        authUtil.checkAuthentication(session);
        return teamsService.getUserTeams();
    }
    
    @GetMapping("/teams/{teamId}/channels")
    @ResponseBody
    public List<ChannelDto> getTeamChannels(@PathVariable String teamId, HttpSession session) {
        authUtil.checkAuthentication(session);
        return teamsService.getTeamChannels(teamId);
    }
    
    @PostMapping("/teams/{teamId}/channels")
    @ResponseBody
    public ChannelDto createChannel(
            @PathVariable String teamId,
            @RequestBody ChannelCreateRequest request,
            HttpSession session) {
        authUtil.checkAuthentication(session);
        return teamsService.createChannel(teamId, request);
    }
    
    @GetMapping("/teams/{teamId}/channels/{channelId}/messages")
    @ResponseBody
    public List<MessageDto> getChannelMessages(
            @PathVariable String teamId,
            @PathVariable String channelId,
            HttpSession session) {
        authUtil.checkAuthentication(session);
        return teamsService.getChannelMessages(teamId, channelId);
    }
}

