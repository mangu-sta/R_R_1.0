package com.release.rr.global.redis.dao;

import com.release.rr.domain.characters.entity.CharacterEntity;
import com.release.rr.domain.characters.repository.CharacterRepository;
import com.release.rr.global.redis.dto.CharacterStateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CharacterStateRedisDao {

    private final HashOperations<String, String, Object> hashOps;
    private final StringRedisTemplate redisTemplate;
    private final CharacterRepository characterRepository;

    private String key(Long id) {
        return "RR:CHAR:" + id;
    }

    private String mapCharsKey(String nanoId) {
        return "RR:MAP:" + nanoId + ":CHARS";
    }

    // 캐릭터 상태 저장(프론트 → 백엔드 → Redis 스냅샷)
    public void saveState(Long id, CharacterStateDto state) {
        hashOps.putAll(key(id), state.toMap());
    }

    // 캐릭터 상태 조회(백엔드 → 프론트 초기화/복구에서 사용)
    public CharacterStateDto getState(Long id) {
        Map<String, Object> map = hashOps.entries(key(id));
        return (map == null || map.isEmpty()) ? null : CharacterStateDto.from(map);
    }

    public CharacterStateDto getByUserId(Long characterId) {
        return getState(characterId);
    }

    // 특정 캐릭터 상태 삭제
    public void deleteState(Long id) {
        hashOps.getOperations().delete(key(id));
    }

    public void createOrUpdate(Long characterId, float x, float y) {

        CharacterStateDto state = getState(characterId);

        // ✅ 1️⃣ Redis가 이미 있으면: 위치/연결만 갱신
        if (state != null) {
            state.updatePosition(x, y);
            state.markConnected();
            saveState(characterId, state);
            return;
        }

        // ✅ 2️⃣ Redis 없을 때만 DB fallback
        CharacterEntity character =
                characterRepository.findById(characterId)
                        .orElseThrow();

        // 🔥 status → snapshot
        var stats = character.getStatSnapshot();

        state = CharacterStateDto.builder()
                .characterId(characterId)
                .userId(character.getUser().getUserId())
                .nickname(character.getUser().getNickname()) // ✅ 닉네임 설정

                .x(x)
                .y(y)
                .angle(character.getAngle())

                .hp(character.getHp())
                .maxHp(stats.getMaxHp())      // ✅ 추가
                .attack(stats.getAttack())    // ✅ 추가

                .strength(stats.getStrength())
                .agility(stats.getAgility())
                .health(stats.getHealth())
                .reload(stats.getReload())

                .level(character.getLevel())
                .exp(character.getExp())
                .pendingStatPoints(stats.getPendingPoints())
                .connected(true)
                .build();

        saveState(characterId, state);
    }


    public List<CharacterStateDto> findAllByMap(String nanoId) {

        Set<String> ids = redisTemplate.opsForSet().members(mapCharsKey(nanoId));
        if (ids == null || ids.isEmpty()) return List.of();

        List<CharacterStateDto> result = new ArrayList<>();

        for (String idStr : ids) {
            Long characterId = Long.parseLong(idStr);
            CharacterStateDto state = getState(characterId);
            if (state != null && state.isConnected()) {
                result.add(state);
            }
        }

        return result;
    }


    public void registerToMap(String nanoId, Long characterId) {
        System.out.println("🔥 REGISTER map=" + nanoId + ", character=" + characterId);
        redisTemplate.opsForSet()
                .add(mapCharsKey(nanoId), characterId.toString());
    }

    public void unregisterFromMap(String nanoId, Long characterId) {
        redisTemplate.opsForSet()
                .remove(mapCharsKey(nanoId), characterId.toString());
    }



    public void clearByMap(String nanoId) {

        String mapKey = mapCharsKey(nanoId);
        Set<String> ids = redisTemplate.opsForSet().members(mapKey);

        try {
            if (ids != null) {
                for (String id : ids) {
                    deleteState(Long.parseLong(id));
                }
            }
        } finally {
            // ❗ 맵 인덱스는 무조건 삭제
            redisTemplate.delete(mapKey);
        }

        System.out.println(
                "🧹 CLEAR CHARACTERS nanoId=" + nanoId +
                        ", count=" + (ids == null ? 0 : ids.size())
        );
    }



}
