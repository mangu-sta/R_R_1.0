package com.release.rr.domain.monster.service;

import com.release.rr.global.redis.dao.CharacterStateRedisDao;
import com.release.rr.global.redis.dao.GameProgressRedisDao;
import com.release.rr.global.redis.dao.MonsterStateRedisDao;
import com.release.rr.global.redis.dao.RedisGameRoomDao;
import com.release.rr.global.redis.dto.CharacterStateDto;
import com.release.rr.global.redis.dto.MonsterStateDto;
import com.release.rr.domain.monster.entity.MonsterEntity;

import com.release.rr.domain.map.entity.MapEntity;
import com.release.rr.domain.map.repository.MapRepository;
import com.release.rr.domain.map.service.MapStageProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class Stage1SpawnScheduler {

    private final RedisGameRoomDao redisGameRoomDao;
    private final MapRepository mapRepository;
    private final MonsterSpawnService monsterSpawnService;
    private final MapStageProvider mapStageProvider;
    private final CharacterStateRedisDao characterRedis;
    private final MonsterStateRedisDao monsterRedis;
    private final GameProgressRedisDao gameProgressRedisDao;

    // 예: 2초마다
    @Scheduled(fixedDelay = 2000)
    public void spawnTick() {

        List<String> nanoIds = redisGameRoomDao.findActiveNanoIds();

        for (String nanoId : nanoIds) {

            MapEntity map = mapRepository.findByNanoId(nanoId).orElse(null);

            // 🔥 본게임(Stage1)만
            if (map == null || map.getStage() != 1) continue;

            if (mapStageProvider.isStage1(nanoId)) {
                // 1. 몬스터 처치 수 확인 (Level 2 상당 = 11마리)
                int killCount = gameProgressRedisDao.getKillCount(nanoId);
                boolean isBossReady = killCount >= 11;

                if (isBossReady) {
                    processBossSpawn(map);
                } else {
                    System.out.println("[Stage1SpawnScheduler] spawnTick for nanoId=" + nanoId);
                    monsterSpawnService.spawnStage1ByRule(map);
                }
            }
        }
    }


    private void processBossSpawn(MapEntity map) {
        String nanoId = map.getNanoId();
        
        // 1. 기존 일반/네임드 몹 싹 제거
        List<MonsterStateDto> monsters = monsterRedis.findAllByMap(nanoId);
        boolean bossActive = false;
        
        for (MonsterStateDto m : monsters) {
            if (MonsterEntity.MonsterType.BOSS.name().equals(m.getType())) {
                bossActive = true;
                continue;
            }
            // 보스 아니면 제거
            monsterRedis.remove(nanoId, m.getMonsterId());
        }

        // 2. 보스 소환 (한 번만)
        if (!bossActive) {
            System.out.println("🔥 LEVEL 40 REACHED! Spawning BOSS for nanoId=" + nanoId);
            monsterSpawnService.spawnBoss(map);
        }
    }

}
