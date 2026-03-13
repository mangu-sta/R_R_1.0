package com.release.rr.domain.map.service;

import com.release.rr.domain.map.dto.StageAdvanceResult;
import com.release.rr.domain.map.repository.MapRepository;
import com.release.rr.domain.monster.service.MonsterSpawnPointProvider;
import com.release.rr.domain.monster.service.MonsterSpawnService;
import com.release.rr.global.redis.dao.CharacterStateRedisDao;
import com.release.rr.global.redis.dao.GameProgressRedisDao;
import com.release.rr.global.redis.dao.MonsterStateRedisDao;
import com.release.rr.global.redis.dto.CharacterStateDto;
import com.release.rr.domain.characters.entity.CharacterEntity;
import com.release.rr.domain.characters.repository.CharacterRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MapStageTransitionService {

    private final MapRepository mapRepository;
    private final MonsterStateRedisDao monsterStateRedisDao;
    private final GridService gridService;
    private final MonsterSpawnService monsterSpawnService;
    private final MonsterSpawnPointProvider spawnPointProvider;
    private final GameProgressRedisDao gameProgressRedisDao;
    private final CharacterStateRedisDao characterStateRedisDao;
    private final CharacterRepository characterRepository;

    @Transactional
    public StageAdvanceResult advanceStage0To1(String nanoId) {

        // ✅ 1. Redis stage 변경
        gameProgressRedisDao.setStage(nanoId, 1);
        gameProgressRedisDao.setStage1StartTime(nanoId, System.currentTimeMillis());

        // ✅ 1-2. DB stage 변경
        mapRepository.advanceStage0To1(nanoId);

        // ✅ 2. 튜토리얼 몬스터 Redis만 제거
        monsterStateRedisDao.clearByMap(nanoId);

        // ✅ 3. 캐시 리셋
        gridService.invalidate(nanoId);
        monsterSpawnService.resetBossFlag(nanoId);
        spawnPointProvider.resetStage1Index(nanoId);

        // ✅ 4. 플레이어 회복 및 스테이지 1 스폰 지점(중앙)으로 텔레포트 (Redis & DB 동기화)
        List<CharacterEntity> playerEntities = characterRepository.findByMap_NanoId(nanoId);
        for (CharacterEntity entity : playerEntities) {
            // Redis 업데이트
            CharacterStateDto p = characterStateRedisDao.getState(entity.getId());
            if (p != null) {
                p.setHp(p.getMaxHp()); // 완치
                p.setX(4736f / 2f); // 중앙 X
                p.setY(3456f / 2f); // 중앙 Y
                characterStateRedisDao.saveState(p.getCharacterId(), p);
            }

            // DB 업데이트
            entity.setHp(entity.getMaxHp());
            entity.setPosX(4736f / 2f);
            entity.setPosY(3456f / 2f);
            characterRepository.save(entity);
        }

        return StageAdvanceResult.success(nanoId, 0, 1);
    }

}
