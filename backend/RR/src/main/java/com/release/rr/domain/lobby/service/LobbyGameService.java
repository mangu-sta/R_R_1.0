package com.release.rr.domain.lobby.service;

import com.release.rr.domain.characters.entity.CharacterEntity;
import com.release.rr.domain.characters.repository.CharacterRepository;
import com.release.rr.domain.game.service.GameStartService;
import com.release.rr.domain.lobby.dto.LobbyEventMessage;
import com.release.rr.domain.lobby.dto.LobbyEventType;
import com.release.rr.domain.lobby.dto.LobbyStatus;
import com.release.rr.domain.lobby.dto.StartConfirmResponse;
import com.release.rr.domain.lobby.dto.gameDto.GameStartPayload;
import com.release.rr.domain.lobby.event.GameAutoRestartEvent;
import com.release.rr.domain.lobby.model.LobbyParty;
import com.release.rr.domain.map.entity.MapEntity;
import com.release.rr.domain.map.repository.MapRepository;
import com.release.rr.global.exception.CustomException;
import com.release.rr.global.exception.ErrorCode;
import com.release.rr.global.redis.dao.RedisGameRoomDao;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LobbyGameService {

    private final MapRepository mapRepository;
    private final CharacterRepository characterRepository;
    private final RedisGameRoomDao redisGameRoomDao;
    private final SimpMessagingTemplate messagingTemplate;
    private final GameStartService gameStartService;
    private final LobbyPartyService lobbyPartyService;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public void startGameInternal(String nanoId) {
        lobbyPartyService.markInGame(nanoId);
        System.out.println("STEP 1");

        // 1. 맵 조회
        MapEntity map = mapRepository.findByNanoId(nanoId)
                .orElseThrow(() -> new CustomException(ErrorCode.MAP_NOT_FOUND));

        System.out.println("STEP 2");

    /*    // 2. 방장 검증
        - 게임 시작 수락 구조에서는 "누가 눌렀는지"가 중요하지 않음
        - 전원 수락이 완료되었으므로 방장 검증은 생략
        if (!map.getOwner().getUserId().equals(hostId)) {
            throw new CustomException(ErrorCode.NOT_HOST);
        }
    */

        // 3. 참여 캐릭터 조회 (이 맵에 있는 캐릭터들)

        // 🔥 nanoId(=roomId)를 기준으로 파티 찾기
        // ❗ hostId 기준 조회는 수락 구조에서 논리적으로 맞지 않음
        LobbyParty party = lobbyPartyService.findPartyByNanoId(nanoId);

        if (party == null) {
            throw new CustomException(ErrorCode.NO_PLAYERS);
        }

        List<Long> userIds = party.getMemberIds().stream().toList();

        System.out.println("STEP 3");

        if (userIds.isEmpty()) {
            throw new CustomException(ErrorCode.NO_PLAYERS);
        }

        System.out.println("STEP 4");

        // 4. Redis 게임룸 생성 ⭐
        redisGameRoomDao.createRoom(map, userIds);

        System.out.println("STEP 5");

        // ⭐ 4-1. 실제 게임 시작 처리
        gameStartService.startGame(map, userIds);

        System.out.println("STEP 6");


        // 5. 대기실 WebSocket → 게임 시작 이벤트
        // - 모든 유저에게 동시에 인게임 진입 신호 전송
        messagingTemplate.convertAndSend(
                "/topic/lobby/" + nanoId,
                new LobbyEventMessage(
                        LobbyEventType.GAME_START,
                        new GameStartPayload(nanoId)
                )
        );
    }


    @Transactional
    public void requestStartGame(String nanoId, Long requesterId) {

      /*  if (lobbyPartyService.getLobbyStatus(nanoId) != LobbyStatus.WAITING) {
            throw new CustomException(ErrorCode.GAME_START_ALREADY_IN_PROGRESS);
        }*/

        LobbyParty party = lobbyPartyService.findPartyByUserId(requesterId);
        if (party == null) {
            throw new CustomException(ErrorCode.NO_PLAYERS);
        }

        // 1️⃣ 로비 상태 CONFIRMING으로 변경 (Redis or DB)
        lobbyPartyService.markConfirming(nanoId);

        // 2️⃣ 모든 멤버 상태 PENDING으로 초기화
        lobbyPartyService.initStartConfirm(nanoId, party.getMemberIds());

        // 3️⃣ 로비 전체에 "수락 요청" 브로드캐스트
        messagingTemplate.convertAndSend(
                "/topic/lobby/" + nanoId,
                new LobbyEventMessage(
                        LobbyEventType.GAME_START_REQUEST,
                        requesterId
                )
        );
    }


    @Transactional
    public void confirmStartGame(
            String nanoId,
            Long userId,
            StartConfirmResponse response
    ) {

        // 파티원이 아닌 유저 응답 방지
        LobbyParty party = lobbyPartyService.findPartyByNanoId(nanoId);
        if (party == null || !party.getMemberIds().contains(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }


        // 0️⃣ (권장) CONFIRMING 상태 체크
        if (lobbyPartyService.getLobbyStatus(nanoId) != LobbyStatus.CONFIRMING) {
            return;
        }

        // 중복 신청 방지
        StartConfirmResponse prev =
                lobbyPartyService.getConfirmStatus(nanoId, userId);

        if (prev != StartConfirmResponse.PENDING) {
            return; // 또는 예외
        }


        // 1️⃣ 상태 기록
        lobbyPartyService.recordStartConfirm(nanoId, userId, response);

        // 🔥 1-1️⃣ 수락 브로드캐스트 (UX용)
        if (response == StartConfirmResponse.ACCEPT) {
            messagingTemplate.convertAndSend(
                    "/topic/lobby/" + nanoId,
                    new LobbyEventMessage(
                            LobbyEventType.GAME_START_ACCEPTED,
                            userId   // ✅ 누가 수락했는지
                    )
            );
        }

        // 2️⃣ 거절이면 즉시 종료
        if (response == StartConfirmResponse.REJECT) {
            lobbyPartyService.resetConfirm(nanoId);

            messagingTemplate.convertAndSend(
                    "/topic/lobby/" + nanoId,
                    new LobbyEventMessage(
                            LobbyEventType.GAME_START_REJECTED,
                            userId
                    )
            );
            return;
        }

        // 3️⃣ 전원 수락 여부 체크
        if (!lobbyPartyService.isAllAccepted(nanoId)) {
            return;
        }

        // 🔥🔥🔥 전원 수락 시에만 게임 시작
        startGameInternal(nanoId);
    }


    @EventListener
    @Transactional
    public void onGameAutoRestart(GameAutoRestartEvent event) {
        String roomId = event.roomId();

        // 안전 체크 (선택)
        LobbyParty party = lobbyPartyService.findPartyByNanoId(roomId);
        if (party == null || party.getMemberIds().isEmpty()) {
            return;
        }

        startGameInternal(roomId);
    }


}
