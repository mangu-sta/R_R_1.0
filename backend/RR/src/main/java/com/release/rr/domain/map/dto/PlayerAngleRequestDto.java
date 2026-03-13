package com.release.rr.domain.map.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerAngleRequestDto {

    // ===== 프론트에서 계산한 시선 각도 =====
    private float angle;

    // ===== 프론트 기준 시간 (선택적) =====
    private long timestamp;
}
