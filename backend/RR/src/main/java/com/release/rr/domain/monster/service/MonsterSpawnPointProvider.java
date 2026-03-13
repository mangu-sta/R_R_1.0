package com.release.rr.domain.monster.service;

import com.release.rr.domain.game.model.SpawnPoint;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MonsterSpawnPointProvider {

    /**
     * =========================
     * Stage 0 (튜토리얼) 전용
     * =========================
     */
    public List<SpawnPoint> getTutorialSpawnPoints() {
        return List.of(
                new SpawnPoint(915.5F, 380.25F),
                new SpawnPoint(1300, 800),
                new SpawnPoint(1500, 1200)
        );
    }

    /**
     * =========================
     * Stage 1 (본게임) 전용
     * 고정 위치 순환 스폰
     * =========================
     */
    private static final List<SpawnPoint> STAGE1_POINTS = List.of(
            new SpawnPoint(50, 50),
            new SpawnPoint(50, 2300),
            new SpawnPoint(4700, 50),
            new SpawnPoint(4700, 1700),
            new SpawnPoint(4700, 3400),
            new SpawnPoint(3400, 2300),
            new SpawnPoint(50, 3400),
            new SpawnPoint(50, 1700)
    );

    // nanoId별 인덱스 관리 (멀티 방 대응)
    private final Map<String, Integer> stage1IndexMap = new ConcurrentHashMap<>();

    /**
     * Stage1 몬스터 스폰 위치 (순환)
     */
    public SpawnPoint pickNextStage1Point(String nanoId) {

        int index = stage1IndexMap.getOrDefault(nanoId, 0);
        SpawnPoint point = STAGE1_POINTS.get(index);

        int nextIndex = (index + 1) % STAGE1_POINTS.size();
        stage1IndexMap.put(nanoId, nextIndex);

        return point;
    }

    /**
     * =========================
     * Boss 전용 스폰 위치
     * =========================
     * - 중앙 / 연출용 고정 위치
     */
    public SpawnPoint pickBossPoint() {
        return new SpawnPoint(
                4736f / 2f,   // center X
                3456f / 2f    // center Y
        );
    }

    /**
     * 방 종료 / 재시작 시 호출
     */
    public void resetStage1Index(String nanoId) {
        stage1IndexMap.remove(nanoId);
    }
}
