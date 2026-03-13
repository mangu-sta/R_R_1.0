package com.release.rr.global.websocket;

import com.release.rr.domain.characters.entity.CharacterEntity;
import com.release.rr.domain.characters.repository.CharacterRepository;
import com.release.rr.global.redis.dao.CharacterStateRedisDao;
import com.release.rr.global.redis.dao.UserGameStateRedisDao;
import com.release.rr.global.redis.dto.CharacterStateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class GameWebSocketEventListener {

    private final UserGameStateRedisDao userGameStateRedisDao;
    private final CharacterStateRedisDao characterStateRedisDao;
    private final CharacterRepository characterRepository;

    private static final long DISCONNECT_GRACE_SECONDS = 180;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {

        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(event.getMessage());

        if (accessor.getSessionAttributes() == null) return;

        Long userId = (Long) accessor.getSessionAttributes().get("userId");
        if (userId == null) return;

        // 1️⃣ disconnect 타이머 시작
        userGameStateRedisDao.markDisconnected(userId, DISCONNECT_GRACE_SECONDS);

        // 2️⃣ 해당 유저의 캐릭터 조회
        CharacterEntity character =
                characterRepository.findByUser_UserId(userId)
                        .orElse(null);

        if (character == null) return;

        // 3️⃣ 캐릭터 상태를 FREEZE 처리
        CharacterStateDto state =
                characterStateRedisDao.getState(character.getId());

        if (state != null) {
            state.markDisconnected();
            characterStateRedisDao.saveState(character.getId(), state);
        }
    }
}
