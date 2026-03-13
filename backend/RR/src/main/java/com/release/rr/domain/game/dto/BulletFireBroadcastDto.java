package com.release.rr.domain.game.dto;

import lombok.Getter;

@Getter
public class BulletFireBroadcastDto {

    private Long userId;
    private GameEventType type;

    private float playerX;
    private float playerY;

    private float muzzleX;
    private float muzzleY;

    private float angle;
    private float range;

    private float targetX;
    private float targetY;

    private long serverTime;

    public static BulletFireBroadcastDto from(BulletFireRequestDto req) {
        BulletFireBroadcastDto dto = new BulletFireBroadcastDto();


        dto.userId = req.getUserId();
        dto.type = GameEventType.BULLET;
        dto.playerX = req.getPlayerX();
        dto.playerY = req.getPlayerY();

        dto.muzzleX = req.getMuzzleX();
        dto.muzzleY = req.getMuzzleY();

        dto.angle = req.getAngle();
        dto.range = req.getRange();

        dto.targetX = req.getTargetX();
        dto.targetY = req.getTargetY();

        dto.serverTime = System.currentTimeMillis();
        return dto;
    }
}


