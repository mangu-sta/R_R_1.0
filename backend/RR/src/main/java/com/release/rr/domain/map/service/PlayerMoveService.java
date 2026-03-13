package com.release.rr.domain.map.service;

import com.release.rr.domain.characters.entity.CharacterEntity;
import com.release.rr.domain.characters.repository.CharacterRepository;
import com.release.rr.domain.game.dto.GameEventType;
import com.release.rr.domain.map.dto.PlayerMoveBroadcastDto;
import com.release.rr.domain.map.dto.PlayerMoveRequestDto;
import com.release.rr.global.exception.CustomException;
import com.release.rr.global.exception.ErrorCode;
import com.release.rr.global.redis.dao.CharacterStateRedisDao;
import com.release.rr.global.redis.dto.CharacterStateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerMoveService {

    private final CharacterRepository characterRepository;
    private final CharacterStateRedisDao characterStateRedisDao;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 프론트 좌표 신뢰 기반 이동 처리
     */
    public void applyMoveInput(String nanoId, Long userId, PlayerMoveRequestDto request) {

        CharacterEntity character =
                characterRepository.findByUser_UserId(userId)
                        .orElseThrow(() ->
                                new CustomException(ErrorCode.CHARACTER_NOT_FOUND));

        CharacterStateDto state =
                characterStateRedisDao.getState(character.getId());

        if (state == null) {
            // 아직 게임 시작 중 → 입력 무시
            return;
        }



        // 1️⃣ 프론트 좌표 반영
        if (System.currentTimeMillis() % 1000 < 100) { // 1초에 한 번만 로그 출력
            System.out.println("[PlayerMoveService] SYNC - nanoId=" + nanoId + 
                    ", userId=" + userId + ", characterId=" + character.getId() + 
                    " -> MOVE TO (x=" + request.getX() + ", y=" + request.getY() + ")");
        }
        state.updatePosition(request.getX(), request.getY());

        // 2️⃣ 입력 상태 반영 (input 객체)
        PlayerMoveRequestDto.Input input = request.getInput();

        state.updateMoveInput(
                input.isUp(),
                input.isDown(),
                input.isLeft(),
                input.isRight(),
                input.isRunning()
        );

        characterStateRedisDao.saveState(character.getId(), state);

        // 3️⃣ 즉시 브로드캐스트 (연출용 방향 포함)
        messagingTemplate.convertAndSend(
                "/topic/game/" + nanoId,
                PlayerMoveBroadcastDto.builder()
                        .type(GameEventType.MOVE)
                        .userId(userId)
                        .x(state.getX())
                        .y(state.getY())
                        .up(state.isUp())
                        .down(state.isDown())
                        .left(state.isLeft())
                        .right(state.isRight())
                        .isRunning(state.isRunning())
                        .build()
        );

    }
}
