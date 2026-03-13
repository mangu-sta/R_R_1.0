package com.release.rr.domain.combat.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MonsterHpUpdateEvent {
    private final String type = "MONSTER_HP_UPDATE"; // ✅ 메시지 식별용 타입 추가
    private Long monsterId;
    private float hp;
    private boolean dead;
    private String state; // "IDLE"/"CHASE"/"ATTACK"/"DEAD"
}
