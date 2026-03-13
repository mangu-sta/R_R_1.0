package com.release.rr.domain.combat.service;

import com.release.rr.domain.combat.dto.req.MonsterHitRequest;
import com.release.rr.domain.combat.dto.res.MonsterHpUpdateEvent;
import com.release.rr.global.exception.CustomException;
import com.release.rr.global.exception.ErrorCode;
import com.release.rr.global.redis.dao.CharacterStateRedisDao;
import com.release.rr.global.redis.dao.RedisGameRoomDao;
import com.release.rr.global.redis.dto.CharacterStateDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CombatFacadeService {

    private final CharacterStateRedisDao characterStateRedisDao;
    private final MonsterDamageService monsterDamageService;
    private final CombatValidationService validationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisGameRoomDao redisGameRoomDao;

    /*
    // ===== REST 진입용 =====
    public void handleMonsterHit(String nanoId, HttpServletRequest httpReq, MonsterHitRequest req) {
        Long userId = extractUserIdFromHttp(httpReq); // 너희 Security/JWT 방식에 맞게 구현
        CharacterStateDto attacker = findAttackerStateByUserId(nanoId, userId);

        MonsterHpUpdateEvent event = monsterDamageService.applyMonsterHit(nanoId, attacker, req, validationService);
        if (event != null) {
            messagingTemplate.convertAndSend("/topic/game/" + nanoId, event);
        }
    }
*/
    // ===== WS 진입용 =====
    public void handleMonsterHit(String nanoId, MonsterHitRequest req) {

        Long userId = req.getUserId();
        if (userId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // 1️⃣ 이 룸에 속한 유저인지 확인
        if (!redisGameRoomDao.isUserInRoom(nanoId, userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // 2️⃣ 이 유저의 캐릭터가 맞는지 확인
        Long characterId = redisGameRoomDao.getCharacterId(nanoId, userId);

        CharacterStateDto attacker =
                characterStateRedisDao.getState(characterId);

        if (attacker == null) {
            throw new CustomException(ErrorCode.CHARACTER_NOT_FOUND);
        }

        MonsterHpUpdateEvent event = monsterDamageService.applyMonsterHit(nanoId, attacker, req, validationService);
        if (event != null) {
            messagingTemplate.convertAndSend("/topic/game/" + nanoId, event);
        }
    }

    public void handleStatSelection(String nanoId, com.release.rr.domain.combat.dto.req.StatSelectionRequest req) {
        Long userId = req.getUserId();
        if (userId == null || !redisGameRoomDao.isUserInRoom(nanoId, userId)) {
            return;
        }

        Long characterId = redisGameRoomDao.getCharacterId(nanoId, userId);
        CharacterStateDto player = characterStateRedisDao.getState(characterId);

        if (player == null || player.getPendingStatPoints() <= 0) {
            return;
        }

        // 1: Strength, 2: Agility, 3: Health, 4: Reload
        switch (req.getStatIndex()) {
            case 1: player.setStrength(player.getStrength() + 1); break;
            case 2: player.setAgility(player.getAgility() + 1); break;
            case 3: player.setHealth(player.getHealth() + 1); break;
            case 4: player.setReload(player.getReload() + 1); break;
            default: return; // 잘못된 인덱스
        }

        player.setPendingStatPoints(player.getPendingStatPoints() - 1);

        // 파생 스탯 재계산 (Health->maxHp, Strength->attack)
        player.setMaxHp(100f + (player.getHealth() * 2f));
        player.setAttack(20f + (player.getStrength() * 2f));

        characterStateRedisDao.saveState(characterId, player);

        // 실시간 반영을 위해 브로드캐스트
        messagingTemplate.convertAndSend("/topic/game/" + nanoId, player);
    }

}
