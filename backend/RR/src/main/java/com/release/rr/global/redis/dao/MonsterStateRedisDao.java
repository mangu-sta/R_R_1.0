package com.release.rr.global.redis.dao;

import com.release.rr.domain.monster.ai.MonsterAiState;
import com.release.rr.domain.monster.entity.MonsterEntity;
import com.release.rr.domain.monster.stat.MonsterStatPreset;
import com.release.rr.global.redis.dto.MonsterStateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MonsterStateRedisDao {

    // ✅ 해시 저장/조회용
    private final HashOperations<String, String, Object> hashOps;

    // ✅ 맵 인덱스(Set)용 (타입 문제 해결 핵심)
    private final StringRedisTemplate redisTemplate;

    // 몬스터 상태 키
    private String key(Long monsterId) {
        return "RR:MON:" + monsterId;
    }

    // map 인덱스 키
    private String mapKey(String nanoId) {
        return "RR:MAP:" + nanoId + ":MONSTERS";
    }

    // ✅ ID 발급 키
    private static final String MONSTER_SEQ = "RR:SEQ:MONSTER";

    private Long generateMonsterId() {
        Long id = redisTemplate.opsForValue().increment(MONSTER_SEQ);
        if (id == null)
            throw new IllegalStateException("몬스터 ID 생성 실패");
        return id;
    }

    // 몬스터 상태 저장 (프론트 스냅샷 → Redis)
    public void save(Long monsterId, MonsterStateDto state) {
        if (monsterId == null) {
            System.err.println("[MonsterStateRedisDao] ERROR: Attempted to save monster with NULL ID!");
            return;
        }
        hashOps.putAll(key(monsterId), state.toMap());
    }

    /**
     * ✅ AI/이동 정보만 부분 업데이트 (HP 덮어쓰기 방지)
     * MonsterTickService에서 사용
     */
    public void updateAiState(Long monsterId, MonsterStateDto state) {
        if (monsterId == null)
            return;

        // ✅ 이미 삭제된(죽은) 몬스터라면 업데이트하지 않음 (유령 몬스터 생성 방지)
        if (Boolean.FALSE.equals(hashOps.getOperations().hasKey(key(monsterId)))) {
            // System.out.println("👻 [GHOST PREVENTED] Monster " + monsterId + " is already
            // gone.");
            return;
        }

        Map<String, Object> map = new java.util.HashMap<>();
        map.put("x", state.getX());
        map.put("y", state.getY());
        map.put("alive", state.isAlive()); // ✅ 생존 여부 동기화 추가
        map.put("state", state.getState());
        map.put("targetCharacterId", state.getTargetCharacterId());
        map.put("targetUserId", state.getTargetUserId());

        // 패턴 정보 포함
        map.put("patternType", state.getPatternType());
        map.put("patternState", state.getPatternState());
        map.put("patternStartTime", state.getPatternStartTime());
        map.put("telegraphX", state.getTelegraphX());
        map.put("telegraphY", state.getTelegraphY());

        // 경로 및 기타 캐시
        map.put("nextPathTime", state.getNextPathTime());
        map.put("pathIndex", state.getPathIndex());
        map.put("path", state.getPath());
        map.put("lastAttackTime", state.getLastAttackTime());
        map.put("lastX", state.getLastX());
        map.put("lastY", state.getLastY());
        map.put("stuckSince", state.getStuckSince());
        map.put("lastTargetTileX", state.getLastTargetTileX());
        map.put("lastTargetTileY", state.getLastTargetTileY());

        hashOps.putAll(key(monsterId), map);
    }

    // 몬스터 상태 조회 (백엔드에서 사용 가능)
    public MonsterStateDto get(Long monsterId) {
        Map<String, Object> map = hashOps.entries(key(monsterId));
        return (map == null || map.isEmpty()) ? null : MonsterStateDto.from(map);
    }

    // 몬스터 상태 삭제
    public void delete(Long monsterId) {
        redisTemplate.delete(key(monsterId));
        // ⚠️ 맵 인덱스에서 제거까지 하려면 nanoId가 필요하므로,
        // 보통 deleteByMap(nanoId, monsterId) 같은 메서드를 따로 둠
    }

    // 기본 스폰
    public void spawn(String nanoId, MonsterEntity monster) {
        if (monster.getId() == null) {
            System.err.println(
                    "[MonsterStateRedisDao] ERROR: spawn() called with monster having NULL ID! nanoId=" + nanoId);
            return;
        }
        MonsterStateDto state = MonsterStateDto.fromSpawn(monster);
        save(monster.getId(), state);

        redisTemplate.opsForSet()
                .add(mapKey(nanoId), monster.getId().toString());
        System.out.println("[MonsterStateRedisDao] spawn SUCCESS: nanoId=" + nanoId + ", monsterId=" + monster.getId());
    }

    // 랜덤 좀비 스폰
    public Long spawnRandomZombie(String nanoId, float x, float y) {
        Long monsterId = generateMonsterId();

        MonsterStateDto state = MonsterStateDto.zombie(monsterId, x, y);
        save(monsterId, state);

        redisTemplate.opsForSet().add(mapKey(nanoId), monsterId.toString());
        System.out.println("🔥MONSTERS " + monsterId + " 생성");

        return monsterId;
    }

    public List<MonsterStateDto> findAllByMap(String nanoId) {
        String key = mapKey(nanoId);
        Set<String> ids = redisTemplate.opsForSet().members(key);

        if (ids == null || ids.isEmpty()) {
            // System.out.println("[MonsterStateRedisDao] findAllByMap: No monsters found
            // for nanoId=" + nanoId + " (Key: " + key + ")");
            return List.of();
        }

        List<MonsterStateDto> activeMonsters = ids.stream()
                .map(id -> get(Long.parseLong(id)))
                .filter(Objects::nonNull)
                .toList();

        // System.out.println("[MonsterStateRedisDao] findAllByMap: nanoId=" + nanoId +
        // ", found " + activeMonsters.size() + " monsters");
        return activeMonsters;
    }

    public List<MonsterStateDto> findAllAliveByMap(String nanoId) {

        Set<String> ids = redisTemplate.opsForSet().members(mapKey(nanoId));

        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return ids.stream()
                // monsterId → MonsterStateDto 조회
                .map(id -> get(Long.parseLong(id)))
                // null 방어
                .filter(Objects::nonNull)
                // 살아있는 몬스터만
                .filter(MonsterStateDto::isAlive)
                .toList();
    }

    public void clearByMap(String nanoId) {

        String mapKey = mapKey(nanoId);
        Set<String> ids = redisTemplate.opsForSet().members(mapKey);

        try {
            if (ids != null) {
                for (String id : ids) {
                    redisTemplate.delete(key(Long.parseLong(id)));
                }
            }
        } finally {
            // ❗ 반드시 인덱스 제거
            redisTemplate.delete(mapKey);
        }

        System.out.println(
                "🧹 CLEAR MONSTERS nanoId=" + nanoId +
                        ", count=" + (ids == null ? 0 : ids.size()));
    }

    public void spawnEphemeral(
            String nanoId,
            float x,
            float y,
            MonsterStatPreset stat,
            MonsterEntity.MonsterType type,
            MonsterEntity.MonsterName name) {
        Long monsterId = generateMonsterId();

        MonsterStateDto state = MonsterStateDto.builder()
                .monsterId(monsterId)
                .x(x)
                .y(y)
                .alive(true)
                .hp(stat.getMaxHp())
                .maxHp(stat.getMaxHp())
                .type(type.name())
                .name(name.name())
                .speed(stat.getSpeed())
                .damage(stat.getDamage())
                .attackSpeed(stat.getAttackSpeed())
                .range(stat.getRange())
                .exp(stat.getExp())
                .state(MonsterAiState.IDLE.name())
                .build();

        save(monsterId, state);
        redisTemplate.opsForSet().add(mapKey(nanoId), monsterId.toString());
    }

    public int countAliveByMap(String nanoId) {
        return findAllAliveByMap(nanoId).size();
    }

    public void remove(String nanoId, Long monsterId) {

        // 1️⃣ 몬스터 상태 해시 제거
        redisTemplate.delete(key(monsterId));

        // 2️⃣ 맵 인덱스(Set)에서 제거
        redisTemplate.opsForSet()
                .remove(mapKey(nanoId), monsterId.toString());
    }

}
