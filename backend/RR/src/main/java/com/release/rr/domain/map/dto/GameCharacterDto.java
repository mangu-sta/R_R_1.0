package com.release.rr.domain.map.dto;

import com.release.rr.domain.characters.entity.CharacterEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class GameCharacterDto {

    private Long characterId;
    private Long userId;
    private String nickname;
    private String job;

    private float posX;
    private float posY;

    private float hp;
    private float maxHp;

    public static GameCharacterDto from(CharacterEntity entity) {
        return GameCharacterDto.builder()
                .characterId(entity.getId())
                .userId(entity.getUser().getUserId())
                .nickname(entity.getUser().getNickname())
                .job(entity.getJob().name())
                .posX(entity.getPosX())
                .posY(entity.getPosY())
                .hp(entity.getHp())
                .maxHp(entity.getMaxHp())
                .build();
    }
}
