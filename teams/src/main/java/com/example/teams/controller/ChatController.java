package com.example.teams.controller;

import com.example.teams.dto.*;
import com.example.teams.service.ChatService;
import com.example.teams.util.AuthUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Microsoft Teams Chat 관련 API 컨트롤러
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/chats")
public class ChatController {
    
    private final ChatService chatService;
    private final AuthUtil authUtil;
    
    @GetMapping
    @ResponseBody
    public List<ChatDto> getChats(HttpSession session) {
        authUtil.checkAuthentication(session);
        return chatService.getChats();
    }
    
    @GetMapping("/{chatId}/messages")
    @ResponseBody
    public List<ChatMessageDto> getChatMessages(@PathVariable String chatId, HttpSession session) {
        authUtil.checkAuthentication(session);
        return chatService.getChatMessages(chatId);
    }
    
    @PostMapping("/{chatId}/messages")
    @ResponseBody
    public ChatMessageDto sendChatMessage(
            @PathVariable String chatId,
            @RequestBody ChatMessageSendRequest request,
            HttpSession session) {
        authUtil.checkAuthentication(session);
        return chatService.sendChatMessage(chatId, request);
    }
    
    @GetMapping("/{chatId}/members")
    @ResponseBody
    public List<ChatMemberDto> getChatMembers(@PathVariable String chatId, HttpSession session) {
        authUtil.checkAuthentication(session);
        return chatService.getChatMembers(chatId);
    }
}

