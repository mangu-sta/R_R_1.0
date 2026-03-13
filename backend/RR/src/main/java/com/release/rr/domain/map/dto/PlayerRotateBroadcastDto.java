package com.release.rr.domain.map.dto;

import com.release.rr.domain.game.dto.GameEventType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerRotateBroadcastDto {
    private GameEventType type; // ROTATE
    private Long userId;
    private float angle;
}
