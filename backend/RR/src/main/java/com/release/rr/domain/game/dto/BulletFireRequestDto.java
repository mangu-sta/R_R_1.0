package com.release.rr.domain.game.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BulletFireRequestDto {

    private Long userId;

    // 플레이어 위치
    private float playerX;
    private float playerY;

    // 총구 위치
    private float muzzleX;
    private float muzzleY;

    // 발사 정보
    private float angle;
    private float range;

    // 조준 지점
    private float targetX;
    private float targetY;
}

