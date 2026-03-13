package com.release.rr.domain.game.service;

import com.release.rr.domain.characters.entity.CharacterEntity;
import com.release.rr.domain.characters.repository.CharacterRepository;
import com.release.rr.domain.map.entity.MapEntity;
import com.release.rr.domain.monster.entity.MonsterEntity;
import com.release.rr.domain.monster.repository.MonsterRepository;
import com.release.rr.global.redis.dao.CharacterStateRedisDao;
import com.release.rr.global.redis.dao.MonsterStateRedisDao;
import com.release.rr.global.redis.dto.CharacterStateDto;
import com.release.rr.global.redis.dto.MonsterStateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameSaveService {

    private final CharacterStateRedisDao characterStateRedisDao;
    private final MonsterStateRedisDao monsterStateRedisDao;

    private final CharacterRepository characterRepository;
    private final MonsterRepository monsterRepository;

    /**
     * 🔥 메인 진입점
     * Redis → DB 저장
     */
    @Transactional
    public void saveGame(MapEntity map, List<Long> userIds) {

        if (map.getStage() == 0) {
            saveTutorial(map, userIds);
        } else {
            saveStage2(map, userIds);
        }
    }

    /**
     * =========================
     * 튜토리얼(Stage 0)
     * =========================
     */
    private void saveTutorial(MapEntity map, List<Long> userIds) {

        savePlayers(userIds);
        saveTutorialMonsters(map);

        System.out.println("💾 SAVE TUTORIAL COMPLETE map=" + map.getNanoId());
    }

    /**
     * =========================
     * 스테이지2(Stage 1)
     * =========================
     */
    private void saveStage2(MapEntity map, List<Long> userIds) {

        savePlayers(userIds);

        // ❌ 몬스터 저장 안 함
        System.out.println("💾 SAVE STAGE2 PLAYERS ONLY map=" + map.getNanoId());
    }

    /**
     * =========================
     * 플레이어 저장 (공통)
     * =========================
     */
    private void savePlayers(List<Long> userIds) {

        for (Long userId : userIds) {

            CharacterEntity character =
                    characterRepository.findByUser_UserId(userId)
                            .orElseThrow();

            Long characterId = character.getId();

            CharacterStateDto state =
                    characterStateRedisDao.getState(characterId);

            if (state == null) continue;

            // 위치
            character.setPosX(state.getX());
            character.setPosY(state.getY());

            // 체력
            character.setHp(Math.max(0, state.getHp()));

            // 진행도
            character.setLevel(state.getLevel());
            character.setExp(state.getExp());
            
            // 모든 스탯 및 포인트 JSON 저장 (status 컬럼 활용)
            String statusJson = com.release.rr.domain.characters.mapper.CharacterStatMapper.toStatusJson(
                state.getStrength(), state.getAgility(), state.getHealth(), state.getReload(), state.getPendingStatPoints()
            );
            character.setStatus(statusJson);

            characterRepository.save(character);
        }
    }

    /**
     * =========================
     * 튜토리얼 몬스터 저장
     * =========================
     */
    private void saveTutorialMonsters(MapEntity map) {

        String nanoId = map.getNanoId();

        List<MonsterStateDto> monsters =
                monsterStateRedisDao.findAllByMap(nanoId);

        for (MonsterStateDto state : monsters) {

            MonsterEntity monster =
                    monsterRepository.findById(state.getMonsterId())
                            .orElse(null);

            if (monster == null) continue;

            monster.setPosX(state.getX());
            monster.setPosY(state.getY());

            // 죽으면 hp=0 유지
            monster.setHp(Math.max(0, state.getHp()));

            monsterRepository.save(monster);
        }
    }
}
