package com.release.rr.domain.map.service;

import com.release.rr.domain.characters.entity.CharacterEntity;
import com.release.rr.domain.characters.repository.CharacterRepository;
import com.release.rr.domain.game.dto.GameEventType;
import com.release.rr.domain.map.dto.PlayerAngleBroadcastDto;
import com.release.rr.domain.map.dto.PlayerAngleRequestDto;
import com.release.rr.global.exception.CustomException;
import com.release.rr.global.exception.ErrorCode;
import com.release.rr.global.redis.dao.CharacterStateRedisDao;
import com.release.rr.global.redis.dto.CharacterStateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerAngleService {

    private final CharacterRepository characterRepository;
    private final CharacterStateRedisDao characterStateRedisDao;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 프론트 시선(angle) 신뢰 기반 처리
     */
    public void applyAngleInput(String nanoId, Long userId, PlayerAngleRequestDto request) {

        // 1️⃣ 캐릭터 조회
        CharacterEntity character =
                characterRepository.findByUser_UserId(userId)
                        .orElseThrow(() ->
                                new CustomException(ErrorCode.CHARACTER_NOT_FOUND));

        // 2️⃣ Redis 상태 조회
        CharacterStateDto state =
                characterStateRedisDao.getState(character.getId());

        if (state == null) {
            throw new CustomException(ErrorCode.CHARACTER_NOT_FOUND);
        }

        // 3️⃣ 프론트 angle 그대로 반영
        state.updateAngle(request.getAngle());

        characterStateRedisDao.saveState(character.getId(), state);

        // 4️⃣ 즉시 브로드캐스트
        messagingTemplate.convertAndSend(
                "/topic/game/" + nanoId,
                new PlayerAngleBroadcastDto(
                        GameEventType.ANGLE,
                        userId,
                        state.getAngle()
                )
        );
    }
}
