package com.release.rr.domain.monster.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MonsterHitRequest {
    private Long monsterId;
    private float damage;
    private Long userId;
}
