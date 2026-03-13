package com.release.rr.domain.lobby.dto;

import lombok.Data;

@Data
public class PartyLeaveOrKickRequest {
    private String targetNickname; // null → 본인 나가기, not null → 추방 로직
}
