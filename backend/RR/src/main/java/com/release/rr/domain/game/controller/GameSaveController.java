package com.release.rr.domain.game.controller;

import com.release.rr.domain.game.dto.GameEventType;
import com.release.rr.domain.game.dto.GameSaveResultMessage;
import com.release.rr.domain.game.service.GameSaveService;
import com.release.rr.domain.map.entity.MapEntity;
import com.release.rr.domain.map.repository.MapRepository;
import com.release.rr.global.exception.CustomException;
import com.release.rr.global.exception.ErrorCode;
import com.release.rr.global.redis.dao.CharacterStateRedisDao;
import com.release.rr.global.redis.dto.CharacterStateDto;
import com.release.rr.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/game")
public class GameSaveController {

    private final GameSaveService gameSaveService;
    private final MapRepository mapRepository;
    private final CharacterStateRedisDao characterStateRedisDao;
    private final SimpMessagingTemplate messagingTemplate;


    @PostMapping("/save")
    public void saveGame() {

        // 1️⃣ 로그인 체크
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // 2️⃣ 현재 유저 소유 맵 조회
        MapEntity map = mapRepository.findByOwnerUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.MAP_NOT_FOUND));

        String nanoId = map.getNanoId();

        // 3️⃣ 현재 맵에 존재하는 캐릭터 상태 조회 (Redis 기준)
        List<CharacterStateDto> states =
                characterStateRedisDao.findAllByMap(nanoId);

        if (states.isEmpty()) {
            throw new CustomException(ErrorCode.NO_PLAYERS);
        }

        // 4️⃣ userId 목록 추출
        List<Long> userIds = states.stream()
                .map(CharacterStateDto::getUserId)
                .distinct()
                .toList();

        if (userIds.isEmpty()) {
            throw new CustomException(ErrorCode.NO_PLAYERS);
        }

        // 5️⃣ 저장 실행
        gameSaveService.saveGame(map, userIds);

        // ✅ 저장 성공 브로드캐스트
        messagingTemplate.convertAndSend(
                "/topic/game/" + nanoId,
                GameSaveResultMessage.builder()
                        .type(GameEventType.GAME_SAVE_SUCCESS)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }
}

