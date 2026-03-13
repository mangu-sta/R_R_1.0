package com.release.rr.domain.monster.stat;

import com.release.rr.domain.monster.entity.MonsterEntity;

public class MonsterStatTable {

        public static MonsterStatPreset get(
                        MonsterEntity.MonsterType type,
                        MonsterEntity.MonsterName name) {

                return switch (type) {

                        case NORMAL -> switch (name) {
                                case SLOW -> new MonsterStatPreset(
                                                120f, // hp
                                                5f, // damage
                                                1.0f, // attackSpeed
                                                120f, // speed (상향)
                                                50f, // range
                                                10 // exp
                                        );
                                case RUNNER -> new MonsterStatPreset(
                                                80f, 4f, 1.2f, 180f, 40f, 15);
                                default -> throw new IllegalStateException("Unknown NORMAL monster");
                        };

                        case NAMED -> new MonsterStatPreset(
                                        300f, 12f, 0.8f, 130f, 70f, 100);

                        case BOSS -> new MonsterStatPreset(
                                        300f, // hp (Testing balance)
                                        60f, // damage
                                        0.5f, // attackSpeed
                                        130f, // speed
                                        120f, // range
                                        5000 // exp
                                );
                };
        }
}
