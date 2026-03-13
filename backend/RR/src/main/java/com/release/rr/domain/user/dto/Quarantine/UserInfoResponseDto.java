package com.release.rr.domain.user.dto.Quarantine;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInfoResponseDto {

    private Long userId;
    private String nickname;
    private String email;
    private int level;
    private String createdAt;
}
