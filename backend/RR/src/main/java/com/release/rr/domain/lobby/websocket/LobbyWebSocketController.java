package com.release.rr.domain.lobby.websocket;

import com.release.rr.domain.lobby.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class LobbyWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    // -----------------------------
    //  공통 유저 정보 조회
    // -----------------------------
    private String getSessionNickname(SimpMessageHeaderAccessor accessor) {
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs == null) return null;
        return (String) attrs.get("nickname");
    }

    // -----------------------------
    //   대기실 채팅 메시지 송신
    //   (클라이언트 → 서버)
    //   /app/lobby/{roomId}/chat
    // -----------------------------
    @MessageMapping("/lobby/{roomId}/chat")
    public void chat(@DestinationVariable String roomId,
                     @Payload LobbyChatRequestDto req,
                     SimpMessageHeaderAccessor accessor) {

        // ⭐ 1순위: WebSocket 세션 닉네임
        String nickname = getSessionNickname(accessor);

        // ⭐ 2순위: 프론트에서 보낸 닉네임 (fallback)
        if (nickname == null || nickname.isBlank()) {
            nickname = req.getNickname();
        }

        // ❗ 그래도 없으면 방어
        if (nickname == null || nickname.isBlank()) {
            nickname = "UNKNOWN";
        }

        LobbyChatMessageDto outbound = LobbyChatMessageDto.builder()
                .nickname(nickname)
                .message(req.getMessage())
                .sentAt(LocalDateTime.now())
                .build();

        // ----------------------------------------
        //  서버 → 클라이언트
        //  /topic/lobby/{roomId}
        // ----------------------------------------
        messagingTemplate.convertAndSend(
                "/topic/lobby/" + roomId,
                new LobbyEventMessage(
                        LobbyEventType.MESSAGE,
                        outbound
                )
        );
    }
}
