package com.release.rr.domain.game.service;

import com.release.rr.domain.characters.entity.CharacterEntity;
import com.release.rr.domain.characters.repository.CharacterRepository;
import com.release.rr.domain.game.model.SpawnPoint;
import com.release.rr.domain.map.entity.MapEntity;
import com.release.rr.domain.map.service.GridService;
import com.release.rr.domain.monster.repository.MonsterRepository;
import com.release.rr.domain.monster.service.MonsterSpawnService;
import com.release.rr.domain.monster.service.MonsterTickService;
import com.release.rr.global.redis.dao.CharacterStateRedisDao;
import com.release.rr.global.redis.dao.GameProgressRedisDao;
import com.release.rr.global.redis.dao.MonsterStateRedisDao;
import com.release.rr.global.redis.dao.RedisGameRoomDao;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameStartService {

    private final CharacterStateRedisDao characterStateRedisDao;
    private final MonsterSpawnService monsterSpawnService;
    private final CharacterRepository characterRepository;
    private final RedisGameRoomDao redisGameRoomDao;
    private final MonsterStateRedisDao monsterStateRedisDao;
    private final StringRedisTemplate redisTemplate;
    private final MonsterTickService monsterTickService;

    // ✅ 저장 여부 판단을 DB에서 하기 위해 추가
    private final MonsterRepository monsterRepository;
    private final GridService gridService;
    private final GameProgressRedisDao gameProgressRedisDao;

    public void startGame(MapEntity map, List<Long> userIds) {

        String nanoId = map.getNanoId();
        // 1️⃣ Redis 초기화
        clearRoomRedis(nanoId, userIds);

        // 2️⃣ 게임 진행 상태 초기화 (DB 상태와 동기화)
        gameProgressRedisDao.setStage(nanoId, map.getStage());
        gameProgressRedisDao.setKillCount(nanoId, map.getKillCount());

        System.out.println("[GameStartService] SYNC FROM DB nanoId=" + nanoId +
                ", stage=" + map.getStage() + ", killCount=" + map.getKillCount());

        // 1️⃣ 현재 stage를 Redis 기준으로 읽는다
        int stage = gameProgressRedisDao.getStage(nanoId);

        // ✅ DB에 튜토리얼 몬스터가 있으면 = 저장된 게임(불러오기)
        boolean hasTutorialMonsterInDb = monsterRepository.existsByMap_MapId(map.getMapId());

        // =========================
        // 튜토리얼 (Stage 0)
        // =========================
        if (stage == 0) {

            if (hasTutorialMonsterInDb) {
                // LOAD
                restorePlayersFromDb(userIds, nanoId);
                monsterSpawnService.restoreTutorialMonstersFromDb(map);
            } else {
                // NEW
                spawnPlayersAtSpawnPoint(userIds, nanoId);
                monsterSpawnService.spawnTutorialMonstersFirstTime(map);
                System.out.println(
                        "[DEBUG] GAME STATE = " +
                                redisTemplate.opsForHash()
                                        .entries("RR:GAME:" + nanoId + ":STATE"));
            }
            System.out.println("[DEBUG-B] BEFORE return = " +
                    redisTemplate.opsForHash().entries("RR:GAME:" + nanoId + ":STATE"));

            return;
        }

        // =========================
        // 본게임 (Stage 1)
        // =========================
        if (stage == 1) {
            // Stage 1은 몬스터가 DB에 저장되지 않으므로 캐릭터 좌표 기반으로 복원
            restorePlayersFromDb(userIds, nanoId);

            // 2️⃣ Stage1 맵은 장애물 구조가 다르므로 Grid 캐시 폐기
            gridService.invalidate(nanoId);

            // ❗ 몬스터는 여기서 생성하지 않음
            // → MonsterTickService가 KillCount / 상한선 기준으로 생성

            return;
        }
    }

    private void spawnPlayersAtSpawnPoint(List<Long> userIds, String nanoId) {
        System.out.println("userIds : " + userIds);
        List<SpawnPoint> spawnPoints = List.of(
                new SpawnPoint(915.5f, 180.25f),
                new SpawnPoint(945.5f, 180.25f),
                new SpawnPoint(975.5f, 180.25f),
                new SpawnPoint(1005.5f, 180.25f));

        // ✅ 스테이지 1용 중앙 스폰 지점
        List<SpawnPoint> stage1SpawnPoints = List.of(
                new SpawnPoint(4736f / 2f, 3456f / 2f),
                new SpawnPoint(4736f / 2f + 30, 3456f / 2f),
                new SpawnPoint(4736f / 2f, 3456f / 2f + 30),
                new SpawnPoint(4736f / 2f + 30, 3456f / 2f + 30));

        int stage = gameProgressRedisDao.getStage(nanoId);
        List<SpawnPoint> targetPoints = (stage == 1) ? stage1SpawnPoints : spawnPoints;

        for (int i = 0; i < userIds.size(); i++) {
            Long userId = userIds.get(i);
            SpawnPoint point = targetPoints.get(i % targetPoints.size());

            CharacterEntity character = characterRepository.findByUser_UserId(userId)
                    .orElseThrow();

            Long characterId = character.getId();

            // ✅ 룸 ↔ 캐릭터 매핑
            redisGameRoomDao.putCharacter(nanoId, userId, characterId);

            // ✅ 캐릭터 상태 생성
            characterStateRedisDao.createOrUpdate(
                    characterId,
                    point.x(),
                    point.y());

            // ✅ 맵 등록
            characterStateRedisDao.registerToMap(nanoId, characterId);

        }
    }

    private void restorePlayersFromDb(List<Long> userIds, String nanoId) {
        for (Long userId : userIds) {
            CharacterEntity character = characterRepository.findByUser_UserId(userId).orElseThrow();
            Long characterId = character.getId();

            redisGameRoomDao.putCharacter(nanoId, userId, characterId);

            // ✅ 저장된 좌표/체력으로 복원 (CharacterEntity에 lastX/lastY/hp 등이 있어야 함)
            characterStateRedisDao.createOrUpdate(
                    characterId,
                    character.getPosX(),
                    character.getPosY()
            // 필요하면 hp도 같이
            );

            characterStateRedisDao.registerToMap(nanoId, characterId);
        }
    }

    private void clearRoomRedis(String nanoId, List<Long> userIds) {
        System.out.println("🧹 CLEAR REDIS ROOM nanoId=" + nanoId);

        // 1️⃣ 맵에 등록된 몬스터 상태 제거
        monsterStateRedisDao.clearByMap(nanoId);

        // 2️⃣ 맵에 등록된 캐릭터 상태 제거
        characterStateRedisDao.clearByMap(nanoId);

        // 3️⃣ 룸 ↔ 유저/캐릭터 매핑 제거
        // redisGameRoomDao.clearRoom(nanoId);

        // 4️⃣ 게임 시작 플래그 제거 (있다면)
        redisTemplate.delete("game:started:" + nanoId);
    }
}
