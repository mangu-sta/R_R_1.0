package com.release.rr.domain.lobby.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LobbyEventMessage {

    private LobbyEventType type;  // 이벤트 타입 (추방, 나가기, 초대수락 등)
    private Object value;         // 실제 데이터 payload
}
