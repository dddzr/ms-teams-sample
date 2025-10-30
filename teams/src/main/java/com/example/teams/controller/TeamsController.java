package com.example.teams.controller;

import com.example.teams.dto.ChannelDto;
import com.example.teams.dto.MessageDto;
import com.example.teams.dto.TeamDto;
import com.example.teams.dto.UserDto;
import com.example.teams.service.TeamsService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api")
public class TeamsController {
    
    private final TeamsService teamsService;
    
    /**
     * 현재 사용자 정보 조회
     */
    @GetMapping("/me")
    @ResponseBody
    public UserDto getCurrentUser(HttpSession session) {
        checkAuthentication(session);
        return teamsService.getCurrentUser();
    }
    
    /**
     * 사용자의 Teams 목록 조회
     */
    @GetMapping("/teams")
    @ResponseBody
    public List<TeamDto> getUserTeams(HttpSession session) {
        checkAuthentication(session);
        return teamsService.getUserTeams();
    }
    
    /**
     * 특정 Team의 채널 목록 조회
     */
    @GetMapping("/teams/{teamId}/channels")
    @ResponseBody
    public List<ChannelDto> getTeamChannels(
            @PathVariable String teamId,
            HttpSession session) {
        checkAuthentication(session);
        return teamsService.getTeamChannels(teamId);
    }
    
    /**
     * 특정 채널의 메시지 조회
     */
    @GetMapping("/teams/{teamId}/channels/{channelId}/messages")
    @ResponseBody
    public List<MessageDto> getChannelMessages(
            @PathVariable String teamId,
            @PathVariable String channelId,
            HttpSession session) {
        checkAuthentication(session);
        return teamsService.getChannelMessages(teamId, channelId);
    }
    
    private void checkAuthentication(HttpSession session) {
        String accessToken = (String) session.getAttribute("accessToken");
        if (accessToken == null) {
            throw new RuntimeException("인증되지 않은 사용자입니다");
        }
    }
}

