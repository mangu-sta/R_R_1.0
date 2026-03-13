package com.release.rr.domain.friend_requests.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FriendRequestCancelDto {
    private Long receiverId; // 요청을 취소할 대상자
}
