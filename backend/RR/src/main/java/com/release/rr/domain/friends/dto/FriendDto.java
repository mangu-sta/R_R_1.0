package com.release.rr.domain.friends.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FriendDto {
    private Long friendId;
    private String nickname;
    private Integer level;
    private String since;   // 언제 친구가 됐는지
}

