package com.release.rr.domain.combat.service;

import com.release.rr.domain.combat.dto.res.PlayerHpUpdateEvent;
import com.release.rr.domain.game.dto.GameEventType;
import com.release.rr.global.redis.dao.CharacterStateRedisDao;
import com.release.rr.global.redis.dto.CharacterStateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerDamageService {

    private final CharacterStateRedisDao characterRedisDao;
    private final com.release.rr.global.redis.dao.GameProgressRedisDao gameProgressRedisDao;

    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public PlayerHpUpdateEvent applyPlayerDamage(String nanoId, Long characterId, float damage) {
        CharacterStateDto state = characterRedisDao.getState(characterId);
        if (state == null) return null;
        if (!state.isConnected()) return null;

        float finalDamage = Math.min(damage, 30f);

        boolean dead = state.applyDamage(finalDamage);
        
        // Save state immediately (HP update)
        characterRedisDao.saveState(characterId, state);

        if (dead) {
            System.out.println("💀 [GAME OVER] Player " + characterId + " died. Resetting ALL players in nanoId=" + nanoId);
            
            // 1. Reset Game Progress (Stage, KillCount)
            gameProgressRedisDao.forceInit(nanoId);

            // 2. Fetch All Players in Room & Reset Local State
            java.util.List<CharacterStateDto> allPlayers = characterRedisDao.findAllByMap(nanoId);
            for (CharacterStateDto p : allPlayers) {
                p.setLevel(0);
                p.setExp(0);
                p.setHp(100);
                p.setMaxHp(100);
                p.setX(915.5f);
                p.setY(180.25f);
                characterRedisDao.saveState(p.getCharacterId(), p);
            }
            
            // 3. Broadcast Global GAME_OVER Event
            // This will trigger the client to transition to Lobby
            java.util.Map<String, Object> msg = new java.util.HashMap<>();
            msg.put("type", "GAME_OVER");
            msg.put("victimId", characterId);
            messagingTemplate.convertAndSend("/topic/game/" + nanoId, msg);
        }

        return new PlayerHpUpdateEvent(GameEventType.PLAYER_HP_UPDATE, characterId, state.getHp(), dead);
    }
}
