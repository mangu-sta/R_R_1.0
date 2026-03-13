package com.release.rr.domain.map.websocket;

import com.release.rr.domain.characters.entity.CharacterEntity;
import com.release.rr.domain.characters.repository.CharacterRepository; // 🔧 [추가]
import com.release.rr.domain.game.dto.BulletFireBroadcastDto;
import com.release.rr.domain.game.dto.BulletFireRequestDto;
import com.release.rr.domain.game.dto.GameEventType;
import com.release.rr.domain.map.dto.PlayerAngleRequestDto;
import com.release.rr.domain.map.dto.PlayerMoveRequestDto;
import com.release.rr.domain.map.dto.PlayerRotateRequestDto;
import com.release.rr.domain.map.service.GameInitService;
import com.release.rr.domain.map.service.PlayerAngleService;
import com.release.rr.domain.map.service.PlayerMoveService;
import com.release.rr.domain.map.service.PlayerRotateService;
import com.release.rr.global.exception.CustomException;
import com.release.rr.global.exception.ErrorCode;
import com.release.rr.global.redis.dao.CharacterStateRedisDao;
import com.release.rr.global.redis.dao.RedisGameRoomDao;
import com.release.rr.global.redis.dao.UserGameStateRedisDao;
import com.release.rr.global.redis.dto.CharacterStateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private final RedisGameRoomDao redisGameRoomDao;
    private final UserGameStateRedisDao userGameStateRedisDao;
    private final GameInitService gameInitService;
    private final PlayerMoveService playerMoveService;
    private final CharacterStateRedisDao characterStateRedisDao;
    private final CharacterRepository characterRepository; // 🔧 [추가]
    private final PlayerAngleService  playerAngleService;
    private final PlayerRotateService  playerRotateService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/game/{nanoId}/join")
    public void join(
            @DestinationVariable String nanoId,
            SimpMessageHeaderAccessor accessor
    ) {
        // 0️⃣ 세션 검증
        if (accessor.getSessionAttributes() == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = (Long) accessor.getSessionAttributes().get("userId");
        if (userId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // 🔧 disconnect 유예시간 초과 시 재접속 차단
        if (userGameStateRedisDao.isDisconnectExpired(userId)) {
            throw new CustomException(ErrorCode.GAME_RECONNECT_EXPIRED);
        }

        // 1️⃣ 게임룸 존재 확인
        if (!redisGameRoomDao.exists(nanoId)) {
            throw new CustomException(ErrorCode.INVALID_GAME_ROOM);
        }

        // 2️⃣ 이 방의 멤버인지 확인
        if (!redisGameRoomDao.isPlayerInRoom(nanoId, userId)) {
            throw new CustomException(ErrorCode.PLAYER_NOT_IN_GAME_ROOM);
        }

        // 3️⃣ Redis에 유저 상태 기록
        userGameStateRedisDao.enterGame(userId, nanoId);

        // 4️⃣ INIT 상태 전송 (개인)
        gameInitService.sendInitialStateToUser(nanoId, userId);

        // 🔧 disconnect 타이머 취소
        userGameStateRedisDao.clearDisconnectTimer(userId);

        // 🔧 userId → characterId 조회
        CharacterEntity character = characterRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND));

        Long characterId = character.getId();

        // 🔧 캐릭터 다시 활성화
        CharacterStateDto state = characterStateRedisDao.getState(characterId);
        if (state != null) {
            state.markConnected();
            characterStateRedisDao.saveState(characterId, state);
        }
    }

    @MessageMapping("/game/{nanoId}/move")
    public void move(
            @DestinationVariable String nanoId,
            @Payload PlayerMoveRequestDto request
    ) {
        Long userId = request.getUserId();
        if (userId == null) {
            throw new CustomException(ErrorCode.WEBSOCKET_SESSION_INVALID);
        }

        playerMoveService.applyMoveInput(nanoId, userId, request);
        
        // 디버그용 수신 확인 (임시)
        if (System.currentTimeMillis() % 1000 < 100) {
            System.out.println("[GameWebSocketController] Received Move from userId=" + userId + 
                    " (x=" + request.getX() + ", y=" + request.getY() + ")");
        }
    }



    @MessageMapping("/game/angle")
    public void angle(
            @DestinationVariable String nanoId,
            @Payload PlayerAngleRequestDto request,
            SimpMessageHeaderAccessor accessor
    ) {
        if (accessor.getSessionAttributes() == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = (Long) accessor.getSessionAttributes().get("userId");
        if (userId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        playerAngleService.applyAngleInput(nanoId, userId, request);
    }


    @MessageMapping("/game/{nanoId}/rotate")
    public void rotate(
            @DestinationVariable String nanoId,
            @Payload PlayerRotateRequestDto request
    ) {
        playerRotateService.applyRotate(
                nanoId,
                request.getUserId(),
                request
        );
    }

    @MessageMapping("/game/{nanoId}/fire")
    public void fireBullet(
            @DestinationVariable String nanoId,
            BulletFireRequestDto req
    ) {
        // 서버는 아무 검증 안 함
        // 프론트에서 온 값 그대로 브로드캐스트

        BulletFireBroadcastDto payload =
                BulletFireBroadcastDto.from(req);

        messagingTemplate.convertAndSend(
                "/topic/game/" + nanoId,
                payload
        );
    }





}
