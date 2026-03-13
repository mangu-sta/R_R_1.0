package com.release.rr.domain.map.service;

import com.release.rr.domain.characters.entity.CharacterEntity;
import com.release.rr.domain.characters.repository.CharacterRepository;
import com.release.rr.domain.game.dto.GameEventType;
import com.release.rr.domain.map.dto.PlayerRotateBroadcastDto;
import com.release.rr.domain.map.dto.PlayerRotateRequestDto;
import com.release.rr.global.exception.CustomException;
import com.release.rr.global.exception.ErrorCode;
import com.release.rr.global.redis.dao.CharacterStateRedisDao;
import com.release.rr.global.redis.dto.CharacterStateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerRotateService {

    private final CharacterRepository characterRepository;
    private final CharacterStateRedisDao characterStateRedisDao;
    private final SimpMessagingTemplate messagingTemplate;

    public void applyRotate(
            String nanoId,
            Long userId,
            PlayerRotateRequestDto request
    ) {
        // 1️⃣ 캐릭터 조회
        CharacterEntity character =
                characterRepository.findByUser_UserId(userId)
                        .orElseThrow(() ->
                                new CustomException(ErrorCode.CHARACTER_NOT_FOUND));

        // 2️⃣ Redis 상태 조회
        CharacterStateDto state =
                characterStateRedisDao.getState(character.getId());

        if (state == null) {
            // ❗ 아직 초기화 안 됐으면 무시 (에러 X)
            return;
        }


        // 3️⃣ 각도 갱신
        state.updateAngle(request.getAngle());

        characterStateRedisDao.saveState(character.getId(), state);

        // 4️⃣ 브로드캐스트
        messagingTemplate.convertAndSend(
                "/topic/game/" + nanoId,
                PlayerRotateBroadcastDto.builder()
                        .type(GameEventType.ROTATE)
                        .userId(userId)
                        .angle(state.getAngle())
                        .build()
        );
    }
}
