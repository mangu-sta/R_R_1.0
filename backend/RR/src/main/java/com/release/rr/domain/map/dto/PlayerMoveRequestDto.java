package com.release.rr.domain.map.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerMoveRequestDto {

    private Long userId;
    // ===== 입력 묶음 (프론트 구조 그대로) =====
    private Input input;

    // ===== 프론트 기준 좌표 =====
    private float x;
    private float y;

    // ===== 프론트 기준 시간 =====
    private long timestamp;

    @Getter
    @Setter
    public static class Input {
        private boolean up;
        private boolean down;
        private boolean left;
        private boolean right;

        // 프론트 필드명 그대로 사용
        @JsonProperty("isRunning")
        private boolean isRunning;
    }
}
