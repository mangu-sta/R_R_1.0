package com.release.rr.domain.notifications.dto;

public enum NotificationType {
    FRIEND_REQUEST,   // 친구 요청 도착
    FRIEND_ACCEPT,    // 친구 요청 수락
    FRIEND_REJECT,    // 친구 요청 거절
    FRIEND_CANCEL,     // 내가 보낸 요청이 취소됨
    LOBBY_INVITE,   // 대기실 초대
    CONSOLE         // 기본
}

