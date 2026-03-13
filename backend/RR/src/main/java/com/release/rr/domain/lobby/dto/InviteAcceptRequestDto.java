package com.release.rr.domain.lobby.dto;

import lombok.Data;

@Data
public class InviteAcceptRequestDto {
    private Long partyId;   // 초대한 파티 ID
    private Long inviteId;  // 초대 ID (검증 용도로만 사용)
}
