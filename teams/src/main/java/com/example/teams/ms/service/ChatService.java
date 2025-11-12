package com.example.teams.ms.service;

import com.example.teams.ms.dto.*;
import com.example.teams.ms.util.GraphApiErrorHandler;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Microsoft Teams Chat 관련 API를 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    
    private final GraphClientService graphClientService;
    private final GraphApiErrorHandler errorHandler;
    
    /**
     * 사용자의 채팅 목록 조회
     */
    public List<ChatDto> getChats() {
        try {
            GraphServiceClient graphClient = graphClientService.getGraphClient();
            var chats = graphClient.me().chats().get(requestConfiguration -> {
                requestConfiguration.queryParameters.top = 50;
            });
            
            List<ChatDto> chatList = new ArrayList<>();
            if (chats != null && chats.getValue() != null) {
                chats.getValue().forEach(chat -> {
                    chatList.add(ChatDto.builder()
                        .id(chat.getId())
                        .topic(chat.getTopic())
                        .chatType(chat.getChatType() != null ? chat.getChatType().toString() : "unknown")
                        .createdDateTime(chat.getCreatedDateTime())
                        .webUrl(chat.getWebUrl())
                        .build());
                });
            }
            
            log.info("채팅 목록 조회 완료: {} 개", chatList.size());
            return chatList;
        } catch (Exception e) {
            errorHandler.handle(e, "채팅 목록 조회");
            return new ArrayList<>(); // 도달하지 않음
        }
    }
    
    /**
     * 채팅 메시지 조회
     */
    public List<ChatMessageDto> getChatMessages(String chatId) {
        try {
            GraphServiceClient graphClient = graphClientService.getGraphClient();
            var messages = graphClient.chats().byChatId(chatId).messages()
                .get(requestConfiguration -> {
                    requestConfiguration.queryParameters.top = 50;
                    requestConfiguration.queryParameters.orderby = new String[]{"createdDateTime desc"};
                });
            
            List<ChatMessageDto> messageList = new ArrayList<>();
            if (messages != null && messages.getValue() != null) {
                messages.getValue().forEach(message -> {
                    String from = message.getFrom() != null && message.getFrom().getUser() != null ?
                        message.getFrom().getUser().getDisplayName() : "Unknown";
                    String body = message.getBody() != null && message.getBody().getContent() != null ?
                        message.getBody().getContent() : "";
                    
                    messageList.add(ChatMessageDto.builder()
                        .id(message.getId())
                        .body(body)
                        .from(from)
                        .createdDateTime(message.getCreatedDateTime())
                        .messageType(message.getMessageType() != null ? 
                            message.getMessageType().toString() : "message")
                        .build());
                });
            }
            
            log.info("채팅 메시지 조회 완료: {} 개", messageList.size());
            return messageList;
        } catch (Exception e) {
            errorHandler.handle(e, "채팅 메시지 조회");
            return new ArrayList<>(); // 도달하지 않음
        }
    }
    
    /**
     * 채팅 메시지 전송
     */
    public ChatMessageDto sendChatMessage(String chatId, ChatMessageSendRequest request) {
        try {
            GraphServiceClient graphClient = graphClientService.getGraphClient();
            com.microsoft.graph.models.ChatMessage message = new com.microsoft.graph.models.ChatMessage();
            com.microsoft.graph.models.ItemBody body = new com.microsoft.graph.models.ItemBody();
            body.setContent(request.getBody());
            message.setBody(body);
            
            var sentMessage = graphClient.chats().byChatId(chatId).messages()
                .post(message);
            
            String from = sentMessage.getFrom() != null && sentMessage.getFrom().getUser() != null ?
                sentMessage.getFrom().getUser().getDisplayName() : "Unknown";
            String bodyContent = sentMessage.getBody() != null && sentMessage.getBody().getContent() != null ?
                sentMessage.getBody().getContent() : "";
            
            return ChatMessageDto.builder()
                .id(sentMessage.getId())
                .body(bodyContent)
                .from(from)
                .createdDateTime(sentMessage.getCreatedDateTime())
                .messageType(sentMessage.getMessageType() != null ? 
                    sentMessage.getMessageType().toString() : "message")
                .build();
        } catch (Exception e) {
            errorHandler.handle(e, "채팅 메시지 전송");
            return null; // 도달하지 않음
        }
    }
    
    /**
     * 채팅 멤버 조회
     */
    public List<ChatMemberDto> getChatMembers(String chatId) {
        try {
            GraphServiceClient graphClient = graphClientService.getGraphClient();
            var members = graphClient.chats().byChatId(chatId).members().get();
            
            List<ChatMemberDto> memberList = new ArrayList<>();
            if (members != null && members.getValue() != null) {
                members.getValue().forEach(member -> {
                    String displayName = member.getDisplayName() != null ? member.getDisplayName() : "";
                    String email = "";
                    // ConversationMember에는 직접 email 필드가 없을 수 있음
                    if (member instanceof com.microsoft.graph.models.AadUserConversationMember) {
                        com.microsoft.graph.models.AadUserConversationMember aadMember = 
                            (com.microsoft.graph.models.AadUserConversationMember) member;
                        if (aadMember.getEmail() != null) {
                            email = aadMember.getEmail();
                        }
                    }
                    
                    memberList.add(ChatMemberDto.builder()
                        .id(member.getId())
                        .displayName(displayName)
                        .email(email)
                        .roles(member.getRoles() != null ? String.join(", ", member.getRoles()) : "")
                        .build());
                });
            }
            
            log.info("채팅 멤버 조회 완료: {} 개", memberList.size());
            return memberList;
        } catch (Exception e) {
            errorHandler.handle(e, "채팅 멤버 조회");
            return new ArrayList<>(); // 도달하지 않음
        }
    }
    
    /**
     * 채팅 생성
     */
    public ChatDto createChat(ChatCreateRequest request) {
        try {
            GraphServiceClient graphClient = graphClientService.getGraphClient();
            
            // Chat 객체 생성
            com.microsoft.graph.models.Chat chat = new com.microsoft.graph.models.Chat();
            chat.setChatType(com.microsoft.graph.models.ChatType.valueOf(
                request.getChatType() != null ? request.getChatType().toUpperCase() : "ONE_ON_ONE"));
            
            // 멤버 목록 생성
            List<com.microsoft.graph.models.AadUserConversationMember> members = new ArrayList<>();
            if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
                for (String userId : request.getUserIds()) {
                    com.microsoft.graph.models.AadUserConversationMember member = 
                        new com.microsoft.graph.models.AadUserConversationMember();
                    member.setOdataType("#microsoft.graph.aadUserConversationMember");
                    member.setRoles(List.of("owner"));
                    member.setAdditionalData(java.util.Map.of(
                        "user@odata.bind", 
                        "https://graph.microsoft.com/v1.0/users('" + userId + "')"
                    ));
                    members.add(member);
                }
            }

            // 기존 lint 에러 수정: setMembers(List<ConversationMember>) expects List<ConversationMember>
            List<com.microsoft.graph.models.ConversationMember> conversationMembers = new ArrayList<>(members);
            chat.setMembers(conversationMembers);

            // 채팅 생성
            var createdChat = graphClient.chats().post(chat);

            return ChatDto.builder()
                .id(createdChat.getId())
                .topic(createdChat.getTopic())
                .chatType(createdChat.getChatType() != null ? createdChat.getChatType().toString() : "unknown")
                .createdDateTime(createdChat.getCreatedDateTime())
                .webUrl(createdChat.getWebUrl())
                .build();
        } catch (Exception e) {
            errorHandler.handle(e, "채팅 생성");
            return null; // 도달하지 않음
        }
    }
}

