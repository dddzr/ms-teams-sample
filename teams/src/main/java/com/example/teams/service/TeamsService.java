package com.example.teams.service;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.example.teams.dto.*;
import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TeamsService {
    
    private GraphServiceClient graphClient;
    
    /**
     * Access Token으로 Graph Client 초기화
     */
    public void initializeGraphClient(String accessToken) {
        try {
            // Access Token을 사용하는 커스텀 TokenCredential 생성
            TokenCredential tokenCredential = new TokenCredential() {
                @Override
                public Mono<AccessToken> getToken(TokenRequestContext request) {
                    // 1시간 후 만료 설정 (실제로는 토큰의 만료 시간을 사용해야 함)
                    OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(1);
                    return Mono.just(new AccessToken(accessToken, expiresAt));
                }
            };
            
            graphClient = new GraphServiceClient(tokenCredential);
                
            log.info("Graph Client 초기화 완료");
        } catch (Exception e) {
            log.error("Graph Client 초기화 실패", e);
            throw new RuntimeException("Graph Client 초기화 실패", e);
        }
    }
    
    /**
     * 현재 사용자 정보 조회
     */
    public UserDto getCurrentUser() {
        try {
            User user = graphClient.me().get();
            
            return UserDto.builder()
                .id(user.getId())
                .displayName(user.getDisplayName())
                .mail(user.getMail())
                .userPrincipalName(user.getUserPrincipalName())
                .jobTitle(user.getJobTitle())
                .build();
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패", e);
            throw new RuntimeException("사용자 정보 조회 실패", e);
        }
    }
    
    /**
     * 사용자가 속한 모든 Teams 조회
     */
    public List<TeamDto> getUserTeams() {
        try {
            log.info("Teams 목록 조회 시작...");
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
        } catch (com.microsoft.graph.models.odataerrors.ODataError e) {
            log.error("Teams 조회 실패 - OData 에러: {}", e.getMessage(), e);
            // 라이선스 문제인 경우 빈 리스트 반환
            if (e.getMessage() != null && e.getMessage().contains("license")) {
                log.warn("Teams 라이선스가 없거나 가입된 팀이 없습니다. 빈 리스트를 반환합니다.");
                return new ArrayList<>();
            }
            throw new RuntimeException("Teams 조회 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Teams 조회 실패 - 일반 에러", e);
            throw new RuntimeException("Teams 조회 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 특정 Team의 채널 목록 조회
     */
    public List<ChannelDto> getTeamChannels(String teamId) {
        try {
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
            log.error("채널 조회 실패 - Team ID: {}", teamId, e);
            throw new RuntimeException("채널 조회 실패", e);
        }
    }
    
    /**
     * 특정 채널의 메시지 조회
     */
    public List<MessageDto> getChannelMessages(String teamId, String channelId) {
        try {
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
            log.error("메시지 조회 실패 - Team: {}, Channel: {}", teamId, channelId, e);
            throw new RuntimeException("메시지 조회 실패", e);
        }
    }
}

