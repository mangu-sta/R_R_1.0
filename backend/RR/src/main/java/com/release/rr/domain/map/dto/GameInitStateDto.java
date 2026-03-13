package com.release.rr.domain.map.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class GameInitStateDto {
    private final String type = "INIT";

    private List<GameCharacterDto> players;
    private List<GameMonsterDto> monsters;
}
