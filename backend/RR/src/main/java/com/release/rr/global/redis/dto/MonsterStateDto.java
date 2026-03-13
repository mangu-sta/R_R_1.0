package com.release.rr.global.redis.dto;

import com.release.rr.domain.monster.ai.MonsterAiState;
import com.release.rr.domain.monster.entity.MonsterEntity;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonsterStateDto {

    /*
     * =========================
     * Identity
     * =========================
     */
    private Long monsterId;
    private String type; // NORMAL, NAMED, BOSS
    private String name; // RUNNER, SLOW, etc.

    /*
     * =========================
     * Position
     * =========================
     */
    private float x;
    private float y;

    /*
     * =========================
     * Status
     * =========================
     */
    private float hp;
    private float maxHp;
    private boolean alive;
    private float speed;
    private float damage;
    private float attackSpeed;
    private float range;
    private int exp;

    /*
     * =========================
     * AI / Behavior
     * =========================
     */
    private String state; // IDLE, CHASE, ATTACK, DEAD, PATTERN
    private Long targetCharacterId;
    private Long targetUserId; // ⭐ 프론트 동기화용 (유저 PK)

    // --- Boss Patterns ---
    private String patternType; // DASH, SWING, SLAM
    private String patternState; // NONE, READY, TELEGRAPH, ACTION
    private long patternStartTime;
    private float telegraphX;
    private float telegraphY;

    /*
     * =========================
     * Pathfinding Cache
     * =========================
     */
    private Long nextPathTime; // millis
    private int pathIndex;
    private String path; // "x,y;x,y;..."

    // 몬스터 추격용 좌표
    private int lastTargetTileX;
    private int lastTargetTileY;

    private float lastX;
    private float lastY;
    private long stuckSince;

    private long lastAttackTime;
    /*
     * =========================
     * Factory Methods
     * =========================
     */

    // DB 기반 스폰
    public static MonsterStateDto fromSpawn(MonsterEntity monster) {
        return MonsterStateDto.builder()
                .monsterId(monster.getId())
                .x(monster.getPosX())
                .y(monster.getPosY())

                .hp(monster.getHp())
                .maxHp(monster.getMaxHp())
                .alive(true)

                .type(monster.getType().name())
                .name(monster.getName().name())

                .speed(monster.getSpeed())
                .damage(monster.getDamage())
                .attackSpeed(monster.getAttackSpeed())
                .range(monster.getRange())

                .exp(monster.getExp())
                .state(MonsterAiState.IDLE.name())
                .nextPathTime(System.currentTimeMillis())
                .pathIndex(0)

                .build();
    }

    // 기본 좀비 스폰
    public static MonsterStateDto zombie(
            Long monsterId,
            float x,
            float y) {
        float defaultHp = 100f;

        return MonsterStateDto.builder()
                .monsterId(monsterId)
                .x(x)
                .y(y)
                .hp(defaultHp)
                .maxHp(defaultHp)
                .alive(true)
                .state("IDLE")
                .nextPathTime(System.currentTimeMillis())
                .pathIndex(0)
                .build();
    }

    /*
     * =========================
     * Domain Logic
     * =========================
     */

    public void applyDamage(float damage) {
        if (!alive)
            return;

        this.hp -= damage;
        // 0.1 미만이면 사망으로 간주 (부동소수점 오차 방지)
        if (this.hp <= 0.1f) {
            this.hp = 0;
            this.alive = false;
        }
    }

    /*
     * =========================
     * Redis Serialize
     * =========================
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("monsterId", monsterId);

        map.put("x", x);
        map.put("y", y);

        map.put("hp", hp);
        map.put("maxHp", maxHp);
        map.put("alive", alive);
        map.put("speed", speed);
        map.put("damage", damage);
        map.put("attackSpeed", attackSpeed);
        map.put("range", range);
        map.put("exp", exp);

        map.put("state", state);
        map.put("targetCharacterId", targetCharacterId);
        map.put("targetUserId", targetUserId);

        map.put("type", type);
        map.put("name", name);

        map.put("patternType", patternType);
        map.put("patternState", patternState);
        map.put("patternStartTime", patternStartTime);
        map.put("telegraphX", telegraphX);
        map.put("telegraphY", telegraphY);

        map.put("nextPathTime", nextPathTime);
        map.put("pathIndex", pathIndex);
        map.put("path", path);
        map.put("lastAttackTime", lastAttackTime);

        return map;
    }

    /*
     * =========================
     * Redis → DTO
     * =========================
     */
    public static MonsterStateDto from(Map<String, Object> map) {
        if (map == null || map.isEmpty())
            return null;

        return MonsterStateDto.builder()
                .monsterId(parseLong(map.get("monsterId")))

                .x(parseFloat(map.get("x")))
                .y(parseFloat(map.get("y")))

                .hp(parseFloat(map.get("hp")))
                .maxHp(parseFloat(map.get("maxHp")))
                .alive(parseBoolean(map.get("alive")))
                .speed(parseFloat(map.get("speed")))
                .damage(parseFloat(map.get("damage")))
                .attackSpeed(parseFloat(map.get("attackSpeed")))
                .range(parseFloat(map.get("range")))
                .exp(parseInt(map.get("exp")))

                .state(parseString(map.get("state")))
                .targetCharacterId(parseLong(map.get("targetCharacterId")))
                .targetUserId(parseLong(map.get("targetUserId")))

                .nextPathTime(
                        parseLong(map.get("nextPathTime")) != null
                                ? parseLong(map.get("nextPathTime"))
                                : System.currentTimeMillis())
                .pathIndex(parseInt(map.get("pathIndex")))
                .path(parseString(map.get("path")))
                .lastAttackTime(
                        parseLong(map.get("lastAttackTime")) != null
                                ? parseLong(map.get("lastAttackTime"))
                                : 0L)
                .type(parseString(map.get("type")))
                .name(parseString(map.get("name")))
                .patternType(parseString(map.get("patternType")))
                .patternState(parseString(map.get("patternState")))
                .patternStartTime(parseLong(map.get("patternStartTime")) != null
                        ? parseLong(map.get("patternStartTime"))
                        : 0L)
                .telegraphX(parseFloat(map.get("telegraphX")))
                .telegraphY(parseFloat(map.get("telegraphY")))
                .build();
    }

    /*
     * =========================
     * Parse Utils
     * =========================
     */
    private static float parseFloat(Object v) {
        return v == null ? 0f : Float.parseFloat(v.toString());
    }

    private static int parseInt(Object v) {
        return v == null ? 0 : Integer.parseInt(v.toString());
    }

    private static Long parseLong(Object v) {
        if (v == null)
            return null;
        return Long.parseLong(v.toString());
    }

    private static boolean parseBoolean(Object v) {
        return v != null && Boolean.parseBoolean(v.toString());
    }

    private static String parseString(Object v) {
        return v == null ? null : v.toString();
    }

}
