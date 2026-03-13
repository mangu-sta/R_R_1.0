
package com.release.rr.domain.lobby.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LobbyInvitePayload {

    private Long inviteId;       // 간단히 UUID나 증가 시퀀스, 지금은 hostId+receiverId 조합으로 해도 됨
    private Long hostId;
    private String hostNickname;
    private Long partyId;
}
