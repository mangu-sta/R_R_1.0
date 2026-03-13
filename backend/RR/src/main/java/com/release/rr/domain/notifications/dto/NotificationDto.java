package com.release.rr.domain.notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private NotificationType type;     // FRIEND_REQUEST, LOBBY_INVITE 등
    private Long senderId;
    private String senderNickname;
    private String message;

    private Object data;               // <--- 여기 추가 (초대 관련 payload 전달)

    private LocalDateTime createdAt;
    private boolean read;
}

