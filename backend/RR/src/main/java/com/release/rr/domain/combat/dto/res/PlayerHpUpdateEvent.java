package com.release.rr.domain.combat.dto.res;

import com.release.rr.domain.game.dto.GameEventType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PlayerHpUpdateEvent {
    private GameEventType type;
    private Long characterId;
    private float hp;
    private boolean dead;
}
