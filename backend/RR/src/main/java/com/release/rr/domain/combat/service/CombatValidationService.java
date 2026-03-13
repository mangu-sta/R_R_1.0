package com.release.rr.domain.combat.service;

import com.release.rr.domain.combat.dto.req.MonsterHitRequest;
import com.release.rr.global.redis.dto.CharacterStateDto;
import com.release.rr.global.redis.dto.MonsterStateDto;
import org.springframework.stereotype.Service;

@Service
public class CombatValidationService {

    /**
     * 서버 기준 최대 허용 데미지
     * - 클라이언트가 수천 데미지를 보내는 치팅 방지
     * - 이후 무기/스킬/스탯 시스템이 붙어도 "상한선" 역할
     */
    private static final float MAX_DAMAGE = 200f;

    /**
     * 근접 공격 최대 사거리
     * - 서버 좌표 기준 거리 판정
     * - 클라이언트 좌표 조작 방지
     */
    private static final float MELEE_RANGE = 70f; // 맵 스케일에 맞게 조절

    /**
     * 원거리 공격 최대 사거리
     * - 총기/투사체/마법 계열 공격용
     * - 탄속/투사체 로직 전이라도 최소한의 거리 제한
     */
    private static final float RANGED_RANGE = 400f;

    /**
     * 몬스터 타격 가능 여부 검증
     *
     * 이 메서드는 "공격이 성립할 수 있는 최소 조건"만 검사한다.
     * ❗ 여기서 false면, 데미지 계산 자체를 하지 않는다.
     */
    public boolean canHitMonster(
            CharacterStateDto attacker,
            MonsterStateDto monster,
            MonsterHitRequest req) {

        // =====================================================
        // 1️⃣ 공격자 생존 여부
        // =====================================================
        // - 이미 죽은 플레이어가 공격하는 현상 방지
        // - disconnect 상태인데 공격 패킷만 날리는 경우 차단
        if (!attacker.isAlive()) {
            return false;
        }

        // =====================================================
        // 2️⃣ 대상 몬스터 생존 여부
        // =====================================================
        // - 이미 죽은 몬스터를 여러 번 때리는 중복 타격 방지
        // - 레이턴시로 인해 늦게 도착한 패킷 무시
        if (!monster.isAlive()) {
            return false;
        }

        // =====================================================
        // 3️⃣ 서버 기준 거리 계산
        // =====================================================
        // - 클라이언트가 보낸 좌표는 신뢰하지 않는다
        // - Redis에 저장된 서버 기준 좌표로 거리 계산
        float dx = attacker.getX() - monster.getX();
        float dy = attacker.getY() - monster.getY();

        // 제곱 거리 비교 (sqrt 제거 → 성능 최적화)
        float dist2 = dx * dx + dy * dy;

        // =====================================================
        // 4️⃣ 공격 타입 결정
        // =====================================================
        // - attackType이 null이면 기본 근접 공격으로 간주
        // - 클라이언트가 값을 안 보내도 서버가 안전하게 처리
        boolean isMelee = req.isKnife();

        // =====================================================
        // 5️⃣ 공격 타입별 사거리 적용
        // =====================================================
        // - 근접 / 원거리 공격의 서버 기준 최대 사거리 제한
        // - 벽 관통, 순간 이동 공격 등의 치팅 1차 방어
        float range = isMelee
                ? MELEE_RANGE
                : RANGED_RANGE;

        // =====================================================
        // 6️⃣ 최종 타격 가능 판정
        // =====================================================
        // - 서버 기준 거리 <= 허용 사거리
        // - 만족하지 않으면 공격은 "없던 일"
        return dist2 <= range * range;
    }

    /**
     * 서버 기준 최종 데미지 계산
     *
     * ❗ 클라이언트가 보낸 데미지는 "참고용"이며
     * 서버가 반드시 다시 계산/보정한다.
     */
    public float computeFinalDamage(
            CharacterStateDto attacker,
            MonsterStateDto monster,
            MonsterHitRequest req) {

        // =====================================================
        // 1️⃣ 공격 타입 기반 기본 데미지 결정
        // =====================================================
        float baseDamage;

        if (req.isKnife()) {
            // 근접 공격
            baseDamage = 20f + (attacker.getStrength() * 2f);
        } else {
            // 원거리 공격 (조정)
            baseDamage = 25f;
        }

        // =====================================================
        // 2️⃣ 서버 기준 상한선 적용
        // =====================================================
        return Math.min(baseDamage, MAX_DAMAGE);
    }
}
