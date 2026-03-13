package com.release.rr.domain.combat.dto.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MonsterHitRequest {

    private Long userId;
    // ===== 공격 대상 =====
    private Long monsterId;

    // ===== 클라이언트 기준 좌표 (연출/검증 참고용) =====
    private float userX;
    private float userY;

    private float monsterX;
    private float monsterY;

    // ===== 공격 타입 =====ss
    // true  → 근접 (칼)
    // false → 원거리 (총/마법 등)
    @JsonProperty("isKnife")
    private boolean isKnife;


}
