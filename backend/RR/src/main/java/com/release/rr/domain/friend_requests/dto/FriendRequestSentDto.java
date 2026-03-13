package com.release.rr.domain.friend_requests.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FriendRequestSentDto {

    private Long requestId;
    private Long receiverId;
    private String receiverNickname;
    private String sentAt;
}

