package com.release.rr.domain.monster.stat;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MonsterStatPreset {

    private final float maxHp;
    private final float damage;
    private final float attackSpeed;
    private final float speed;
    private final float range;
    private final int exp;
}

