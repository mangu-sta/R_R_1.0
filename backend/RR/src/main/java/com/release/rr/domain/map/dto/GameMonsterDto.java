package com.release.rr.domain.map.dto;

import com.release.rr.global.redis.dto.MonsterStateDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GameMonsterDto {

    private Long monsterId;
    private float x;
    private float y;
    private float hp;
    private boolean alive;

    public static GameMonsterDto from(MonsterStateDto state) {
        return new GameMonsterDto(
                state.getMonsterId(),
                state.getX(),
                state.getY(),
                state.getHp(),
                state.isAlive()
        );
    }
}

