package com.release.rr.global.redis.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
public class CharacterStateDto {
    private final String type = "PLAYER_STATE";

    private Long characterId; // 캐릭터 PK
    private Long userId;      // ⭐ 유저 PK
    private String nickname;  // ✅ 닉네임 추가

    // ===== 위치 (프론트 신뢰) =====
    private float x;
    private float y;

    // ===== 이동 입력 상태 (연출/동기화용) =====
    private boolean up;
    private boolean down;
    private boolean left;
    private boolean right;
    private boolean isRunning; // ✅ 변경

    // ===== 시선 =====
    private float angle;

    // ===== 전투 핵심 =====
    private float hp;
    private float maxHp;   // ✅ 서버 계산
    private float attack;  // ✅ 서버 계산

    // ===== 스탯 날것 =====
    private int strength;
    private int agility;   // 이동속도 계산용
    private int health;    // ✅ 추가
    private int reload;    // 재장전 계산용

    // ===== 기타 =====
    private int level;
    private int exp;
    private int pendingStatPoints; // ✅ 찍을 수 있는 포인트
    private boolean connected;
    // =================================================
    // 위치 갱신
    // =================================================
    public void updatePosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    // =================================================
    // 이동 입력 갱신
    // =================================================
    public void updateMoveInput(
            boolean up,
            boolean down,
            boolean left,
            boolean right,
            boolean isRunning
    ) {
        this.up = up;
        this.down = down;
        this.left = left;
        this.right = right;
        this.isRunning = isRunning;
    }

    // =================================================
    // 시선 갱신
    // =================================================
    public void updateAngle(float angle) {
        this.angle = angle;
    }

    // =================================================
    // 상태 제어
    // =================================================
    public void markConnected() {
        this.connected = true;
    }

    public void markDisconnected() {
        this.connected = false;
    }

    // =================================================
    // 데미지 처리
    // =================================================
    public boolean applyDamage(float damage) {
        this.hp -= damage;
        if (this.hp <= 0) {
            this.hp = 0;
            return true;
        }
        return false;
    }

    // =================================================
    // 생존 여부 판단 (AI / 전투용)
    // =================================================
    public boolean isAlive() {
        return this.hp > 0 && this.connected;
    }

    // =================================================
    // 레벨업 / 경험치 로직
    // =================================================
    public boolean gainExp(int amount) {
        if (this.level >= 40) return false;

        System.out.println("DEBUG EXP ["+userId+"]: Current Level=" + level + ", Exp=" + exp + ", Gain=" + amount + ", Required=" + getRequiredExp());
        
        this.exp += amount;
        int required = getRequiredExp();
        boolean leveledUp = false;

        while (this.exp >= required && this.level < 40) {
            this.exp -= required;
            this.level++;
            this.pendingStatPoints++; // 레벨업 마다 1포인트
            leveledUp = true;
            System.out.println("DEBUG LEVEL UP ["+userId+"]: Now Level=" + level + ", Remaining Exp=" + exp + ", Next Required=" + getRequiredExp());
            required = getRequiredExp(); // 다음 레벨 필요량
        }
        return leveledUp;
    }

    public int getRequiredExp() {
        // level 0 -> 50 (약 4마리), 이후 레벨업마다 +10 (약 1마리씩 증가)
        return 50 + (this.level * 10);
    }



    // =================================================
    // Redis 저장
    // =================================================
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("type", type);
        map.put("characterId", characterId);
        map.put("userId", userId);
        map.put("nickname", nickname); // ✅ 저장

        map.put("x", x);
        map.put("y", y);

        map.put("up", up);
        map.put("down", down);
        map.put("left", left);
        map.put("right", right);
        map.put("isRunning", isRunning); // ✅ 변경

        map.put("angle", angle);

        map.put("hp", hp);
        map.put("level", level);
        map.put("exp", exp);
        map.put("maxExp", getRequiredExp()); // ✅ 실시간 계산하여 전송
        map.put("pendingStatPoints", pendingStatPoints);
        map.put("connected", connected);
        map.put("maxHp", maxHp);
        map.put("attack", attack);
        map.put("agility", agility);
        map.put("health", health);
        map.put("reload", reload);
        map.put("strength", strength);

        return map;
    }

    // =================================================
    // Redis → DTO
    // =================================================
    public static CharacterStateDto from(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;

        Object statusObj = map.get("status");

        return CharacterStateDto.builder()

                .characterId(parseLong(map.get("characterId")))
                .userId(parseLong(map.get("userId")))
                .nickname((String) map.get("nickname")) // ✅ 복원

                .x(parseFloat(map.get("x")))
                .y(parseFloat(map.get("y")))

                .up(parseBoolean(map.get("up")))
                .down(parseBoolean(map.get("down")))
                .left(parseBoolean(map.get("left")))
                .right(parseBoolean(map.get("right")))
                .isRunning(parseBoolean(map.get("isRunning"))) // ✅ 변경

                .angle(parseFloat(map.get("angle")))

                .hp(parseFloat(map.get("hp")))
                .level(parseInt(map.get("level")))
                .exp(parseInt(map.get("exp")))
                .pendingStatPoints(parseInt(map.get("pendingStatPoints")))
                .connected(parseBoolean(map.get("connected")))
                .maxHp(parseFloat(map.get("maxHp")))
                .attack(parseFloat(map.get("attack")))
                .agility(parseInt(map.get("agility")))
                .health(parseInt(map.get("health")))
                .reload(parseInt(map.get("reload")))
                .strength(parseInt(map.get("strength")))

                .build();
    }

    // =================================================
    // 파싱 유틸
    // =================================================
    private static float parseFloat(Object v) {
        return v == null ? 0f : Float.parseFloat(v.toString());
    }

    private static int parseInt(Object v) {
        return v == null ? 0 : Integer.parseInt(v.toString());
    }

    private static boolean parseBoolean(Object v) {
        return v != null && Boolean.parseBoolean(v.toString());
    }

    private static Long parseLong(Object v) {
        return v == null ? null : Long.parseLong(v.toString());
    }

}
