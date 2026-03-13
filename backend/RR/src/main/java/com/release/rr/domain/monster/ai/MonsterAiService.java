package com.release.rr.domain.monster.ai;

import com.release.rr.global.redis.dto.MonsterStateDto;
import com.release.rr.global.redis.dto.CharacterStateDto;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class MonsterAiService {

    /**
     * 몬스터가 플레이어를 처음 인식할 수 있는 거리
     */
    private static final float DETECT_RANGE = 600f;

    /**
     * 이 거리보다 멀어지면 추격을 포기하고 타겟 해제
     * (전투 이탈 판단 기준)
     */
    private static final float KEEP_RANGE = 2200f;

    /**
     * 공격 상태로 전환되는 거리
     * (실제 공격 판정은 Tick 쪽에서 한 번 더 검사)
     */
    //private static final float ATTACK_RANGE = 50f;

    /**
     * =====================================================
     * 타겟 선정 / 유지 / 해제
     *
     * ❗ 이 메서드는 "target"만 책임진다.
     * ❗ state(IDLE / CHASE / ATTACK)는 절대 건드리지 않는다.
     * =====================================================
     */
    public void updateTarget(
            MonsterStateDto monster,
            List<CharacterStateDto> players,
            int stage
    ) {

        // 0️⃣ 몬스터가 죽어있으면 타겟 해제
        if (!monster.isAlive()) {
            monster.setTargetCharacterId(null);
            return;
        }

        // ✅ stage별 인식 범위 계산
        float detectRange = (stage == 1) ? 10_000f : DETECT_RANGE;
        float keepRange   = (stage == 1) ? 10_000f : KEEP_RANGE;

        /**
         * =========================
         * Stage 1 (본게임)
         * - 가장 가까운 살아있는 플레이어 즉시 타겟
         * =========================
         */
        if (stage == 1) {
            CharacterStateDto nearest = players.stream()
                    .filter(CharacterStateDto::isAlive)
                    .min(Comparator.comparingDouble(p -> distance(monster, p)))
                    .orElse(null);

            monster.setTargetCharacterId(
                    nearest == null ? null : nearest.getCharacterId()
            );
            return;
        }

        /**
         * =========================
         * Stage 0 (튜토리얼)
         * =========================
         */

        // 1️⃣ 기존 타겟 유지 / 해제
        if (monster.getTargetCharacterId() != null) {

            CharacterStateDto currentTarget =
                    findByCharacterId(players, monster.getTargetCharacterId());

            // 타겟 사망/삭제
            if (currentTarget == null || !currentTarget.isAlive()) {
                monster.setTargetCharacterId(null);
                return;
            }

            // ⭐ 너무 멀어졌으면 전투 이탈 → 타겟 해제
            if (distance(monster, currentTarget) > keepRange) {
                monster.setTargetCharacterId(null);
                return;
            }

            // 더 가까운 플레이어 탐색
            CharacterStateDto closer =
                    players.stream()
                            .filter(CharacterStateDto::isAlive)
                            .filter(p -> distance(monster, p) <= detectRange)
                            .min(Comparator.comparingDouble(p -> distance(monster, p)))
                            .orElse(null);

            if (closer != null) {
                float currentDist = distance(monster, currentTarget);
                float closerDist  = distance(monster, closer);

                if (closerDist + 80f < currentDist) {
                    monster.setTargetCharacterId(closer.getCharacterId());
                }
            }

            return;
        }

        // 2️⃣ 신규 타겟 탐색
        CharacterStateDto nearest =
                players.stream()
                        .filter(CharacterStateDto::isAlive)
                        .filter(p -> distance(monster, p) <= detectRange)
                        .min(Comparator.comparingDouble(p -> distance(monster, p)))
                        .orElse(null);

        if (nearest != null) {
            monster.setTargetCharacterId(nearest.getCharacterId());
        }
    }


    /**
     * =====================================================
     * 상태 전이 (IDLE / CHASE / ATTACK)
     *
     * ❗ 이 메서드는 "state"만 책임진다.
     * ❗ target은 절대 수정하지 않는다.
     * =====================================================
     */
    public void updateState(
            MonsterStateDto monster,
            CharacterStateDto target
    ) {

        // 타겟이 없으면 무조건 IDLE
        if (target == null) {
            monster.setState(MonsterAiState.IDLE.name());
            return;
        }

        float dist = distance(monster, target);
        float attackRange = monster.getRange();

        // ⭐ 타겟이 죽었으면 IDLE로 전환
        if (!target.isAlive() || target.getHp() <= 0) {
            monster.setState(MonsterAiState.IDLE.name());
            monster.setTargetCharacterId(null);
            return;
        }

        // 공격 범위 안
        if (dist <= attackRange) {
            monster.setState(MonsterAiState.ATTACK.name());
        } else {
            monster.setState(MonsterAiState.CHASE.name());
        }
    }

    /**
     * 캐릭터 ID로 플레이어 찾기
     */
    private CharacterStateDto findByCharacterId(
            List<CharacterStateDto> players,
            Long characterId
    ) {
        return players.stream()
                .filter(p -> p.getCharacterId() != null)
                .filter(p -> p.getCharacterId().equals(characterId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 몬스터 ↔ 플레이어 거리 계산 (서버 기준)
     */
    private float distance(
            MonsterStateDto m,
            CharacterStateDto p
    ) {
        float dx = p.getX() - m.getX();
        float dy = p.getY() - m.getY();
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
