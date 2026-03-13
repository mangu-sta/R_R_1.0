package com.release.rr.domain.lobby.listener;

import com.release.rr.domain.lobby.dto.LobbyUserDto;
import com.release.rr.domain.lobby.dto.LobbyUserListDto;
import com.release.rr.domain.lobby.service.LobbyService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LobbySessionEventListener {

    private final LobbyService lobbyService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        @SuppressWarnings("unchecked")
        var attributes = (java.util.Map<String, Object>) accessor.getSessionAttributes();

        if (attributes == null) {
            return;
        }

        Long userId = (Long) attributes.get("userId");
        String nickname = (String) attributes.get("nickname");
        Integer level = (Integer) attributes.get("level");

        if (userId == null) {
            return;
        }

        // 대기실 유저 목록에 추가
        lobbyService.addUser(userId, nickname, level);

        // 전체 유저에게 현재 대기실 유저 목록 브로드캐스트
        broadcastUserList();
    }

    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        @SuppressWarnings("unchecked")
        var attributes = (java.util.Map<String, Object>) accessor.getSessionAttributes();

        if (attributes == null) {
            return;
        }

        Long userId = (Long) attributes.get("userId");
        if (userId == null) {
            return;
        }

        // 대기실 유저 목록에서 제거
        lobbyService.removeUser(userId);

        // 전체 유저에게 현재 대기실 유저 목록 브로드캐스트
        broadcastUserList();
    }

    private void broadcastUserList() {
        List<LobbyUserDto> users = lobbyService.getAllUsers().stream().toList();
        LobbyUserListDto dto = LobbyUserListDto.builder()
                .users(users)
                .build();

        // /topic/lobby/users 로 브로드캐스트
        messagingTemplate.convertAndSend("/topic/lobby/users", dto);
    }
}
