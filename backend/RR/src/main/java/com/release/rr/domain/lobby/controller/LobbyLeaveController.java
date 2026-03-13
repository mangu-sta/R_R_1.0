package com.release.rr.domain.lobby.controller;

import com.release.rr.domain.lobby.dto.LobbyEventMessage;
import com.release.rr.domain.lobby.dto.LobbyEventType;
import com.release.rr.domain.lobby.dto.LobbyUpdateValue;
import com.release.rr.domain.lobby.dto.PartyLeaveOrKickRequest;
import com.release.rr.domain.lobby.model.LobbyParty;
import com.release.rr.domain.lobby.service.LobbyPartyService;
import com.release.rr.domain.user.entity.UserEntity;
import com.release.rr.domain.user.repository.UserRepository;
import com.release.rr.global.exception.CustomException;
import com.release.rr.global.exception.ErrorCode;
import com.release.rr.global.security.SecurityUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;   // ⭐ 추가
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/lobby")
@RequiredArgsConstructor
public class LobbyLeaveController {

    private final LobbyPartyService lobbyPartyService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;   // ⭐ 추가

    @PostMapping("/leave")
    public ResponseEntity<?> leaveOrKick(@RequestBody PartyLeaveOrKickRequest request) {

        // JWT claims
        Long requesterId = SecurityUtil.getCurrentUserId();
        String requesterNickname = SecurityUtil.getCurrentNickname();

        if (requesterId == null || requesterNickname == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

       /* Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Claims claims)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Long requesterId = claims.get("userId", Long.class);
        String requesterNickname = claims.get("nickname", String.class);
*/
        // 유저가 속한 파티 찾기
        LobbyParty party = lobbyPartyService.findPartyByUserId(requesterId);
        if (party == null) {
            throw new CustomException(ErrorCode.LOBBY_NOT_FOUND);
        }

        // ⭐ leave/kick 직전, 본인이 구독한 기존 roomId 저장
        String oldRoomId = party.getRoomId();

        // ---------------------------
        // 1) 추방 기능 (targetNickname 존재)
        // ---------------------------
        if (request.getTargetNickname() != null && !request.getTargetNickname().isEmpty()) {

            Long hostId = party.getHostId();
            if (!hostId.equals(requesterId)) {
                throw new CustomException(ErrorCode.NOT_HOST);
            }

            UserEntity targetUser = userRepository.findByNickname(request.getTargetNickname())
                    .orElseThrow(() -> new CustomException(ErrorCode.TARGET_USER_NOT_FOUND));

            Long targetUserId = targetUser.getUserId();

            if (targetUserId.equals(requesterId)) {
                throw new CustomException(ErrorCode.CANNOT_KICK_SELF);
            }

            if (!party.getMemberIds().contains(targetUserId)) {
                throw new CustomException(ErrorCode.TARGET_NOT_IN_PARTY);
            }

            // ⭐ 추방
            lobbyPartyService.leaveParty(party.getPartyId(), targetUserId);

            // ⭐ NEW — 추방된 사용자 기준으로 newRoomId 가져오기
            LobbyParty newPartyOfTarget = lobbyPartyService.findPartyByUserId(targetUserId);
            String newRoomId = (newPartyOfTarget != null) ? newPartyOfTarget.getRoomId() : null;

            // ⭐ 1) 남아 있는 파티원 → 기존 oldRoomId 갱신
            if (oldRoomId != null) {
                simpMessagingTemplate.convertAndSend(
                        "/topic/lobby/" + oldRoomId,
                        new LobbyEventMessage(
                                LobbyEventType.KICK,
                                new LobbyUpdateValue("KICK")
                        )
                );
            }

            // ⭐ 2) 추방된 본인 → newRoomId 갱신
            if (newRoomId != null) {
                simpMessagingTemplate.convertAndSend(
                        "/topic/lobby/" + newRoomId,
                        new LobbyEventMessage(
                                LobbyEventType.KICK,
                                new LobbyUpdateValue("KICK")
                        )
                );
            }

            return ResponseEntity.ok(request.getTargetNickname() + " 님을 추방했습니다.");
        }

        // ---------------------------
        // 2) 본인 나가기 기능
        // ---------------------------

        // ✅ (1) leaveParty 호출 전에 "내가 호스트였는지" 먼저 저장
        boolean wasHost = party.getHostId().equals(requesterId);

        // ✅ (2) 나가기 처리 (여기서 호스트 위임이 일어날 수 있음)
        lobbyPartyService.leaveParty(party.getPartyId(), requesterId);

        // ✅ (3) 내가 호스트였다면: "기존 로비(oldRoomId)"에 게임 종료 신호 전송
        if (wasHost && oldRoomId != null) {
            simpMessagingTemplate.convertAndSend(
                    "/topic/lobby/" + oldRoomId,
                    new LobbyEventMessage(
                            LobbyEventType.HOST_LEFT,
                            null
                    )
            );
        }

        // ⭐ NEW — 나간 본인의 새 방 조회
        String newRoomId;

        // 파티 있으면 파티 room
                LobbyParty newParty = lobbyPartyService.findPartyByUserId(requesterId);
                if (newParty != null) {
                    newRoomId = newParty.getRoomId();
                }
        // 파티 없으면 개인 로비 room (Redis)
                else {
                    newRoomId = lobbyPartyService.getPersonalLobbyRoomId(requesterId);
                }


        // ⭐ 1) 남아 있는 파티원 → 기존 oldRoomId 갱신
        if (oldRoomId != null) {
            simpMessagingTemplate.convertAndSend(
                    "/topic/lobby/" + oldRoomId,
                    new LobbyEventMessage(
                            LobbyEventType.LEAVE,
                            new LobbyUpdateValue("LEAVE")
                    )
            );
        }

        // ⭐ 2) 나간 본인 → newRoomId 갱신
        if (newRoomId != null) {
            simpMessagingTemplate.convertAndSend(
                    "/topic/lobby/" + newRoomId,
                    new LobbyEventMessage(
                            LobbyEventType.LEAVE,
                            new LobbyUpdateValue("LEAVE")
                    )
            );
        }

        return ResponseEntity.ok("파티에서 나갔습니다.");
    }
}
