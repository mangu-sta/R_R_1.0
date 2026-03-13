package com.release.rr.domain.game.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GameSaveResultMessage {

    private GameEventType type;
    private long timestamp;
}
