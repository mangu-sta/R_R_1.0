package com.release.rr.domain.monster.dto;

import com.release.rr.domain.game.dto.GameEventType;
import lombok.*;

import java.util.List;

@Getter
@Builder
public class MonsterStateMessage {
    private GameEventType type;  // "MONSTER_STATE"
    private List<MonsterStateItem> monsters;
    private long timestamp;
}
