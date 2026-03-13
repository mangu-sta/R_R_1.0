package com.release.rr.domain.map.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.release.rr.domain.game.dto.GameEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class PlayerMoveBroadcastDto {

    private GameEventType type; // MOVE
    private Long userId;

    // 서버 계산 결과
    private float x;
    private float y;

    // 이동 연출용 입력 결과
    private boolean up;
    private boolean down;
    private boolean left;
    private boolean right;
    @JsonProperty("isRunning")
    private boolean isRunning;
}


