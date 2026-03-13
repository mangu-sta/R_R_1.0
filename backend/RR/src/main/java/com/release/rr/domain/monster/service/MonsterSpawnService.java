package com.release.rr.domain.monster.service;

import com.release.rr.domain.game.model.SpawnPoint;
import com.release.rr.domain.map.entity.MapEntity;
import com.release.rr.domain.map.service.MapStageProvider;
import com.release.rr.domain.monster.entity.MonsterEntity;
import com.release.rr.domain.monster.repository.MonsterRepository;
import com.release.rr.global.redis.dao.MonsterStateRedisDao;
import com.release.rr.global.redis.dao.RedisGameRoomDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.release.rr.domain.monster.stat.MonsterStatPreset;
import com.release.rr.domain.monster.stat.MonsterStatTable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MonsterSpawnService {

    private final MonsterRepository monsterRepository;
    private final MonsterStateRedisDao monsterStateRedisDao;
    private final MonsterSpawnPointProvider spawnPointProvider;
    private final RedisGameRoomDao redisGameRoomDao;
    private final MapStageProvider mapStageProvider;

    private static final int TUTORIAL_SPAWN_COUNT = 3;


    private final ConcurrentHashMap<String, Boolean> bossSpawned = new ConcurrentHashMap<>();

    private static final int MAX_ALIVE = 1000;

    /**
     * =========================
     * 튜토리얼(Stage 0)
     * =========================
     */

    // 1️⃣ 튜토리얼 최초 시작
    // - 몬스터를 DB에 저장
    // - Redis에도 바로 반영
    public void spawnTutorialMonstersFirstTime(MapEntity map) {

        String nanoId = map.getNanoId();
        System.out.println("🔥 TUTORIAL FIRST SPAWN nanoId=" + nanoId);

        List<SpawnPoint> spawnPoints =
                new java.util.ArrayList<>(spawnPointProvider.getTutorialSpawnPoints());

        Collections.shuffle(spawnPoints);

        spawnPoints.stream()
                .limit(TUTORIAL_SPAWN_COUNT)
                .forEach(p -> {
                    // 1️⃣ DB 저장
                    MonsterStatPreset stat =
                            MonsterStatTable.get(
                                    MonsterEntity.MonsterType.NORMAL,
                                    MonsterEntity.MonsterName.SLOW
                            );

                    MonsterEntity monster = MonsterEntity.builder()
                            .map(map)
                            .type(MonsterEntity.MonsterType.NORMAL)
                            .name(MonsterEntity.MonsterName.SLOW)
                            .posX(p.x())
                            .posY(p.y())

                            // 스탯 확정
                            .hp(stat.getMaxHp())
                            .maxHp(stat.getMaxHp())
                            .damage(stat.getDamage())
                            .attackSpeed(stat.getAttackSpeed())
                            .speed(stat.getSpeed())
                            .range(stat.getRange())
                            .exp(stat.getExp())
                            .build();

                    MonsterEntity saved = monsterRepository.save(monster);
                    monsterStateRedisDao.spawn(nanoId, saved);

                });
    }

    // 2️⃣ 튜토리얼 불러오기
    // - DB → Redis 복원
    public void restoreTutorialMonstersFromDb(MapEntity map) {

        String nanoId = map.getNanoId();
        System.out.println("🔁 RESTORE TUTORIAL MONSTERS nanoId=" + nanoId);

        List<MonsterEntity> monsters =
                monsterRepository.findByMap_MapId(map.getMapId());

        for (MonsterEntity monster : monsters) {
            // ✅ Entity 기반 spawn 사용
            monsterStateRedisDao.spawn(nanoId, monster);
        }
    }


    /**
     * =========================
     * 스테이지2(Stage 1)
     * =========================
     */

    // 3️⃣ 스테이지2 몬스터 스폰 시작 (틱 기반)
    public void spawnStage1ByRule(MapEntity map) {
        String nanoId = map.getNanoId(); // Fix: Declare nanoId from map

        if (!mapStageProvider.isStage1(nanoId)) {
            return;
        }

        int killCount = mapStageProvider.getKillCount(nanoId);
        int aliveCount = monsterStateRedisDao.countAliveByMap(nanoId);

        System.out.println("[MonsterSpawnService] spawnStage1ByRule nanoId=" + nanoId + ", kill=" + killCount + ", alive=" + aliveCount);

        // 1. 일반몹: 항상 15마리 유지
        if (aliveCount < 15) {
            spawnNormal(map);
        }

        // 2. 네임드: 10킬 넘을 때마다 + 살아있는 게 1마리 미만일 때
        if (killCount >= 10 && (killCount % 10 == 0) && aliveCount < 1) { // Added aliveCount < 1 condition based on common game logic for named spawns
            spawnNamed(map);
        }

        // 3. 보스: 200킬 넘으면 1회 스폰
        if (killCount >= 200 && bossSpawned.putIfAbsent(nanoId, true) == null) { // Replaced isBossSpawned with existing bossSpawned map logic
            spawnBoss(map);
        }
    }

    private void spawnNormal(MapEntity map) {
        String nanoId = map.getNanoId();
        System.out.println("[MonsterSpawnService] spawnNormal for map=" + nanoId);
        SpawnPoint p = spawnPointProvider.pickNextStage1Point(nanoId);
        MonsterStatPreset stat = MonsterStatTable.get(
                MonsterEntity.MonsterType.NORMAL,
                MonsterEntity.MonsterName.RUNNER
        );

        monsterStateRedisDao.spawnEphemeral(nanoId, p.x(), p.y(), stat, MonsterEntity.MonsterType.NORMAL, MonsterEntity.MonsterName.RUNNER);
    }

    private void spawnNamed(MapEntity map) {
        String nanoId = map.getNanoId();
        System.out.println("[MonsterSpawnService] spawnNamed for map=" + nanoId);
        SpawnPoint p = spawnPointProvider.pickNextStage1Point(nanoId);
        MonsterStatPreset stat = MonsterStatTable.get(
                MonsterEntity.MonsterType.NAMED,
                MonsterEntity.MonsterName.SLOW // 이름 enum 없으면 아무거나 쓰지말고 NAMED 전용 name 추가 추천
        );

        monsterStateRedisDao.spawnEphemeral(nanoId, p.x(), p.y(), stat, MonsterEntity.MonsterType.NAMED, MonsterEntity.MonsterName.SLOW);
    }

    public void spawnBoss(MapEntity map) {
        String nanoId = map.getNanoId();
        System.out.println("[MonsterSpawnService] spawnBoss for map=" + nanoId);
        SpawnPoint p = spawnPointProvider.pickBossPoint(); // 중앙/특정 위치 추천
        MonsterStatPreset stat = MonsterStatTable.get(
                MonsterEntity.MonsterType.BOSS,
                MonsterEntity.MonsterName.SLOW
        );

        monsterStateRedisDao.spawnEphemeral(nanoId, p.x(), p.y(), stat, MonsterEntity.MonsterType.BOSS, MonsterEntity.MonsterName.SLOW);
    }

    public void resetBossFlag(String nanoId) {
        bossSpawned.remove(nanoId);
    }




}

