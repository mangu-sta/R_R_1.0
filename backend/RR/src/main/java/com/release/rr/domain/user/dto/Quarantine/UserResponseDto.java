package com.release.rr.domain.user.dto.Quarantine;

import com.release.rr.domain.user.entity.UserEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponseDto {

    private Long userId;
    private String nickname;
    private Integer level;
    private String token; // 필요 없으면 삭제해도 됨

    // 🔥 UserEntity -> UserResponseDto 변환 메서드
    public static UserResponseDto from(UserEntity user) {
        return UserResponseDto.builder()
                .userId(user.getUserId())       // ← 정확한 필드
                .nickname(user.getNickname())
                .level(user.getLevel())
                .build();
    }
}
