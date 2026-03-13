package com.release.rr.domain.monster.dto;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonsterStateItem {
    //브로드캐스트용

    private Long id;
    private String type;
    private String name;
    private float x;
    private float y;
    private float hp;
    private float maxHp;

    // "CHASE", "IDLE", "PATTERN", ...
    private String state;

    private Long targetUserId;

    private String patternType;
    private String patternState;
    private float telegraphX;
    private float telegraphY;
}
