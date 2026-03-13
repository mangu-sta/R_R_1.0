package com.release.rr.domain.combat.service;

import com.release.rr.domain.combat.dto.req.MonsterHitRequest;
import com.release.rr.domain.combat.dto.res.MonsterHpUpdateEvent;
import com.release.rr.domain.map.service.MapCommandService;
import com.release.rr.global.redis.dao.GameProgressRedisDao;
import com.release.rr.global.redis.dao.KillCountDedupRedisDao;
import com.release.rr.global.redis.dao.MonsterStateRedisDao;
import com.release.rr.global.redis.dto.CharacterStateDto;
import com.release.rr.global.redis.dto.MonsterStateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MonsterDamageService {

    private final MonsterStateRedisDao monsterRedisDao;
    private final com.release.rr.domain.rank.repository.BossClearRepository bossClearRepository;

    private final MapCommandService mapCommandService;
    private final KillCountDedupRedisDao killCountDedupRedisDao;
    private final GameProgressRedisDao gameProgressRedisDao;
    private final com.release.rr.global.redis.dao.CharacterStateRedisDao characterStateRedisDao;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public MonsterHpUpdateEvent applyMonsterHit(
            String nanoId,
            CharacterStateDto attacker,
            MonsterHitRequest req,
            CombatValidationService validation) {
        MonsterStateDto monster = monsterRedisDao.get(req.getMonsterId());
        if (monster == null || !monster.isAlive())
            return null;
        if (attacker == null || !attacker.isAlive())
            return null;

        // 1️⃣ 서버 검증
        if (!validation.canHitMonster(attacker, monster, req))
            return null;

        // 2️⃣ 데미지 계산
        float damage = validation.computeFinalDamage(attacker, monster, req);
        float oldHp = monster.getHp();
        monster.applyDamage(damage);

        if ("BOSS".equals(monster.getType())) {
            System.out.println("👹 [BOSS HIT] Damage: " + damage + " | HP: " + oldHp + " -> " + monster.getHp()
                    + " (Alive: " + monster.isAlive() + ")");
        }

        // 3️⃣ 사망 처리
        if (!monster.isAlive()) {
            // ⭐ 실피 문제를 방지하기 위해 사망 시 즉시 HP 0 업데이트 전송
            messagingTemplate.convertAndSend("/topic/game/" + nanoId,
                    new MonsterHpUpdateEvent(monster.getMonsterId(), 0, true, "DEAD"));

            monster.setState("DEAD");
            monster.setTargetCharacterId(null);

            // 🌟 Boss Clear Logic
            if ("BOSS".equals(monster.getType())) {
                System.out.println("👹 [BOSS DEATH DETECTED] nanoId=" + nanoId + ", killer=" + attacker.getNickname());
                System.out.println("👹 Current Stage: " + gameProgressRedisDao.getStage(nanoId));
                System.out.println("👹 isStage1: " + (gameProgressRedisDao.getStage(nanoId) == 1));

                long now = System.currentTimeMillis();
                Long startTime = gameProgressRedisDao.getStage1StartTime(nanoId);
                long elapsedSeconds = 0;
                if (startTime != null) {
                    elapsedSeconds = (now - startTime) / 1000;
                }

                // Save Record
                com.release.rr.domain.rank.entity.BossClearRecord record = com.release.rr.domain.rank.entity.BossClearRecord
                        .builder()
                        .nanoId(nanoId)
                        .nickname(attacker.getNickname()) // Assuming nickname exists, or use userId
                        .timeTakenSeconds(elapsedSeconds)
                        .clearedAt(java.time.LocalDateTime.now())
                        .build();
                bossClearRepository.save(record);

                System.out
                        .println("🏆 [BOSS CLEAR] " + attacker.getNickname() + " cleared in " + elapsedSeconds + "s!");

                // Broadcast Boss Killed Event
                java.util.List<String> playerNicknames = characterStateRedisDao.findAllByMap(nanoId).stream()
                        .map(com.release.rr.global.redis.dto.CharacterStateDto::getNickname)
                        .toList();

                java.util.Map<String, Object> msg = new java.util.HashMap<>();
                msg.put("type", "BOSS_KILLED");
                msg.put("killer", attacker.getNickname());
                msg.put("timeTaken", elapsedSeconds);
                msg.put("players", playerNicknames);
                messagingTemplate.convertAndSend("/topic/game/" + nanoId, msg);

                // 몬스터 즉시 제거 및 즉시 종료 (유령 보스 방지)
                monsterRedisDao.remove(nanoId, monster.getMonsterId());
                return new MonsterHpUpdateEvent(monster.getMonsterId(), 0, true, "DEAD");
            }

            // 몬스터 즉시 제거 (일반 몬스터)
            monsterRedisDao.remove(nanoId, monster.getMonsterId());

            int stage = gameProgressRedisDao.getStage(nanoId);

            if (stage == 1) {
                // ✅ 본게임
                if (killCountDedupRedisDao.markOnce(nanoId, monster.getMonsterId())) {
                    gameProgressRedisDao.increaseKill(nanoId);
                    // 🌟 EXP 지급
                    int expAmount = monster.getExp();
                    int oldLevel = attacker.getLevel();
                    int oldExp = attacker.getExp();

                    System.out.println(
                            "⚔️ [BATTLE] Monster Killed: " + monster.getMonsterId() + " | Reward Exp: " + expAmount);

                    if (attacker.gainExp(expAmount)) {
                        System.out.println("🎉 [BATTLE] LEVEL UP [" + attacker.getUserId() + "]: " + oldLevel + " -> "
                                + attacker.getLevel() + " (Points: " + attacker.getPendingStatPoints() + ")");
                    } else {
                        System.out.println("📈 [BATTLE] EXP Gained [" + attacker.getUserId() + "]: " + oldExp + " -> "
                                + attacker.getExp() + " / " + attacker.getRequiredExp());
                    }
                    characterStateRedisDao.saveState(attacker.getCharacterId(), attacker);

                    // 📡 클라이언트에 실시간 경험치/레벨 업데이트 전송
                    messagingTemplate.convertAndSend("/topic/game/" + nanoId, attacker.toMap());
                }

            } else {
                // ✅ 튜토리얼 (Stage 0)
                // 🌟 튜토리얼 몬스터도 경험치 지급 (Stage 1과 동일한 로직 적용)
                int expAmount = monster.getExp();
                int oldLevel = attacker.getLevel();
                int oldExp = attacker.getExp();

                if (attacker.gainExp(expAmount)) {
                    System.out.println("🎉 [TUTORIAL] LEVEL UP [" + attacker.getUserId() + "]: " + oldLevel + " -> "
                            + attacker.getLevel() + " (Points: " + attacker.getPendingStatPoints() + ")");
                } else {
                    System.out.println("📈 [TUTORIAL] EXP Gained [" + attacker.getUserId() + "]: " + oldExp + " -> "
                            + attacker.getExp() + " / " + attacker.getRequiredExp());
                }
                characterStateRedisDao.saveState(attacker.getCharacterId(), attacker);

                // 📡 클라이언트에 실시간 업데이트 전송
                messagingTemplate.convertAndSend("/topic/game/" + nanoId, attacker.toMap());

                monsterRedisDao.save(monster.getMonsterId(), monster);
            }

            return new MonsterHpUpdateEvent(
                    monster.getMonsterId(),
                    0,
                    true,
                    "DEAD");
        }

        // 4️⃣ 생존 상태만 Redis 갱신
        monsterRedisDao.save(monster.getMonsterId(), monster);

        return new MonsterHpUpdateEvent(
                monster.getMonsterId(),
                monster.getHp(),
                false,
                monster.getState());
    }
}
