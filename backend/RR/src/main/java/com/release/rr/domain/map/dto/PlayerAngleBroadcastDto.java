package com.release.rr.domain.map.dto;

import com.release.rr.domain.game.dto.GameEventType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PlayerAngleBroadcastDto {

    private GameEventType type; // ANGLE
    private long userId;
    private float angle;
}
