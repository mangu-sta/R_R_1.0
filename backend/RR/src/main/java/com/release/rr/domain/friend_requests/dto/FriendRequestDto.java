package com.release.rr.domain.friend_requests.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FriendRequestDto {
    private Long senderId;
    private String senderNickname;
    private Long requestId;
    private String sentAt;
}
