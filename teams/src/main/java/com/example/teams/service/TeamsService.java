package com.example.teams.service;

import com.example.teams.dto.*;
import com.example.teams.util.GraphApiErrorHandler;
import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Microsoft Teams 관련 API를 처리하는 서비스
 * Teams, Channels, Channel Messages만 처리합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeamsService {
    
    private final GraphClientService graphClientService;
    private final GraphApiErrorHandler errorHandler;
    
    /**
     * 현재 사용자 정보 조회 (공통 기능)
     */
    public UserDto getCurrentUser() {
        try {
            GraphServiceClient graphClient = graphClientService.getGraphClient();
            User user = graphClient.me().get();
            
            return UserDto.builder()
                .id(user.getId())
                .displayName(user.getDisplayName())
                .mail(user.getMail())
                .userPrincipalName(user.getUserPrincipalName())
                .jobTitle(user.getJobTitle())
                .build();
        } catch (Exception e) {
            errorHandler.handle(e, "사용자 정보 조회");
            return null; // 도달하지 않음
        }
    }
    
    /**
     * 사용자가 속한 모든 Teams 조회
     */
    public List<TeamDto> getUserTeams() {
        try {
            log.info("Teams 목록 조회 시작...");
            GraphServiceClient graphClient = graphClientService.getGraphClient();
            var teams = graphClient.me().joinedTeams().get();
            
            List<TeamDto> teamList = new ArrayList<>();
            if (teams != null && teams.getValue() != null) {
                teams.getValue().forEach(team -> {
                    teamList.add(TeamDto.builder()
                        .id(team.getId())
                        .displayName(team.getDisplayName())
                        .description(team.getDescription())
                        .isArchived(team.getIsArchived() != null ? team.getIsArchived() : false)
                        .webUrl(team.getWebUrl())
                        .build());
                });
            }
            
            log.info("Teams 조회 완료: {} 개", teamList.size());
            return teamList;
        } catch (Exception e) {
            errorHandler.handle(e, "Teams 조회");
            return new ArrayList<>(); // 도달하지 않음
        }
    }
    
    /**
     * 특정 Team의 채널 목록 조회
     */
    public List<ChannelDto> getTeamChannels(String teamId) {
        try {
            GraphServiceClient graphClient = graphClientService.getGraphClient();
            var channels = graphClient.teams().byTeamId(teamId).channels().get();
            
            List<ChannelDto> channelList = new ArrayList<>();
            if (channels != null && channels.getValue() != null) {
                channels.getValue().forEach(channel -> {
                    channelList.add(ChannelDto.builder()
                        .id(channel.getId())
                        .displayName(channel.getDisplayName())
                        .description(channel.getDescription())
                        .webUrl(channel.getWebUrl())
                        .membershipType(channel.getMembershipType() != null ? 
                            channel.getMembershipType().toString() : "standard")
                        .build());
                });
            }
            
            log.info("Team {} 의 채널 조회 완료: {} 개", teamId, channelList.size());
            return channelList;
        } catch (Exception e) {
            errorHandler.handle(e, "채널 조회");
            return new ArrayList<>(); // 도달하지 않음
        }
    }
    
    /**
     * 특정 채널의 메시지 조회
     */
    public List<MessageDto> getChannelMessages(String teamId, String channelId) {
        try {
            GraphServiceClient graphClient = graphClientService.getGraphClient();
            var messages = graphClient.teams().byTeamId(teamId)
                .channels().byChannelId(channelId)
                .messages()
                .get(requestConfiguration -> {
                    requestConfiguration.queryParameters.top = 20;
                });
            
            List<MessageDto> messageList = new ArrayList<>();
            if (messages != null && messages.getValue() != null) {
                messages.getValue().forEach(message -> {
                    String from = message.getFrom() != null && message.getFrom().getUser() != null ? 
                        message.getFrom().getUser().getDisplayName() : "Unknown";
                    String body = message.getBody() != null ? message.getBody().getContent() : "";
                    
                    messageList.add(MessageDto.builder()
                        .id(message.getId())
                        .body(body)
                        .from(from)
                        .createdDateTime(message.getCreatedDateTime())
                        .messageType(message.getMessageType() != null ? 
                            message.getMessageType().toString() : "message")
                        .build());
                });
            }
            
            log.info("채널 메시지 조회 완료: {} 개", messageList.size());
            return messageList;
        } catch (Exception e) {
            errorHandler.handle(e, "채널 메시지 조회");
            return new ArrayList<>(); // 도달하지 않음
        }
    }
    
    /**
     * 채널 생성
     */
    public ChannelDto createChannel(String teamId, ChannelCreateRequest request) {
        try {
            GraphServiceClient graphClient = graphClientService.getGraphClient();
            com.microsoft.graph.models.Channel channel = new com.microsoft.graph.models.Channel();
            channel.setDisplayName(request.getDisplayName());
            channel.setDescription(request.getDescription());
            
            var createdChannel = graphClient.teams().byTeamId(teamId).channels()
                .post(channel);
            
            return ChannelDto.builder()
                .id(createdChannel.getId())
                .displayName(createdChannel.getDisplayName())
                .description(createdChannel.getDescription())
                .webUrl(createdChannel.getWebUrl())
                .membershipType(createdChannel.getMembershipType() != null ? 
                    createdChannel.getMembershipType().toString() : "standard")
                .build();
        } catch (Exception e) {
            errorHandler.handle(e, "채널 생성");
            return null; // 도달하지 않음
        }
    }   
}
