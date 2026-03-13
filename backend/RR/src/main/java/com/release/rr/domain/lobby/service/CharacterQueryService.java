package com.release.rr.domain.lobby.service;

import com.release.rr.domain.characters.dto.CharacterResponseDto;
import com.release.rr.domain.characters.entity.CharacterEntity;
import com.release.rr.domain.characters.service.CharacterService;
import com.release.rr.global.redis.dao.CharacterStateRedisDao;
import com.release.rr.global.redis.dto.CharacterStateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CharacterQueryService {

    private final CharacterStateRedisDao characterStateRedisDao;
    private final CharacterService characterService;

    public CharacterResponseDto getCharacterWithCurrentHp(Long userId) {

        // 1️⃣ userId → 캐릭터 조회 (DB)
        CharacterEntity entity =
                characterService.getCharacterByUserId(userId);

        Long characterId = entity.getId();

        // 2️⃣ DB 기반 DTO
        CharacterResponseDto dto =
                CharacterResponseDto.from(entity);

        // 3️⃣ Redis는 characterId 기준
        CharacterStateDto state =
                characterStateRedisDao.getState(characterId);

        if (state != null) {
            dto.setHp(state.getHp());
            dto.setLevel(state.getLevel());
            dto.setExp(state.getExp());
            dto.setPendingStatPoints(state.getPendingStatPoints());
        } else {
            // Redis에 없으면 DB JSON에서 꺼내옴
            dto.setPendingStatPoints(entity.getStatSnapshot().getPendingPoints());
        }

        return dto;
    }
}
