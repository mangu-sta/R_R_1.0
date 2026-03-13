package com.release.rr.domain.characters.dto;

import com.release.rr.domain.characters.entity.CharacterEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CharacterResponseDto {

    private Long id;
    private Long userId;
    private String nickname;  // 유저 닉네임도 있으면 좋음
    private String job;

    private Integer level;
    private Integer exp;
    private Integer pendingStatPoints;

    private Long mapId;

    private float posX;
    private float posY;

    private Float hp;
    private Float maxHp;

    private String status;

    private LocalDateTime createdAt;

    private Boolean isEnd;




    public static CharacterResponseDto from(CharacterEntity e) {
        CharacterResponseDto dto = new CharacterResponseDto();

        dto.id = e.getId();
        dto.userId = e.getUser().getUserId();
        dto.nickname = e.getUser().getNickname();
        dto.job = e.getJob().name();

        dto.level = e.getLevel();
        dto.exp = e.getExp();

        dto.mapId = e.getMap().getMapId();

        dto.posX = e.getPosX();
        dto.posY = e.getPosY();

        dto.hp = e.getHp();
        dto.maxHp = e.getMaxHp();

        dto.status = e.getStatus();

        dto.createdAt = e.getCreatedAt();
        dto.isEnd = e.getIsEnd();

        return dto;
    }
}
