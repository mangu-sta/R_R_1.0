package com.release.rr.domain.lobby.controller;

import com.release.rr.domain.lobby.dto.InviteRequestDto;
import com.release.rr.domain.lobby.dto.LobbyInvitePayload;
import com.release.rr.domain.lobby.model.LobbyParty;
import com.release.rr.domain.lobby.service.LobbyPartyService;

import com.release.rr.domain.notifications.dto.NotificationDto;
import com.release.rr.domain.notifications.dto.NotificationType;
import com.release.rr.domain.notifications.service.NotificationService;

import com.release.rr.domain.user.entity.UserEntity;
import com.release.rr.domain.user.repository.UserRepository;

import com.release.rr.global.exception.CustomException;
import com.release.rr.global.exception.ErrorCode;

import com.release.rr.global.security.SecurityUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/lobby")
@RequiredArgsConstructor
public class LobbyInviteController {

    private final LobbyPartyService lobbyPartyService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // 🔥 JSON Body 기반 초대 API
    @PostMapping("/invite")
    public ResponseEntity<?> inviteFriend(@RequestBody InviteRequestDto request) {

        // 1) 로그인 Claims 가져오기
        Long hostId = SecurityUtil.getCurrentUserId();
        String hostNickname = SecurityUtil.getCurrentNickname();

        if (hostId == null || hostNickname == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }


        String nickname = request.getNickname();

        // 2) nickname -> friendId 변환
        UserEntity friend = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new CustomException(ErrorCode.INVITE_TARGET_NOT_FOUND));

        Long friendId = friend.getUserId();

        // 3) 자기 자신에게 초대 방지
        if (friendId.equals(hostId)) {
            throw new CustomException(ErrorCode.INVITE_SELF_NOT_ALLOWED);
        }

        // 4) 현재 유저가 속한 파티 찾기 (반드시 이것만 사용해야 함)
        LobbyParty party = lobbyPartyService.findPartyByUserId(hostId);
        if (party == null) {
            throw new CustomException(ErrorCode.NOT_IN_PARTY);
        }

/*

        // 4-1) 초대자가 반드시 파티장인지 검증
        if (!party.getHostId().equals(hostId)) {
            throw new CustomException(ErrorCode.INVITE_NOT_HOST);
        }
*/


        /*
        // ★ 4-2) 초대 대상이 이미 "다른 파티"에 속해있는지 검증
        if (lobbyPartyService.isUserInAnyParty(friendId)) {
            throw new CustomException(ErrorCode.INVITE_ALREADY_IN_PARTY);
        }
        */

        // 5) 파티가 가득 찼는지 확인
        if (party.isFull()) {
            throw new CustomException(ErrorCode.INVITE_PARTY_FULL);
        }

        // 6) 이미 파티원인지 확인
        if (party.getMemberIds().contains(friendId)) {
            throw new CustomException(ErrorCode.INVITE_ALREADY_MEMBER);
        }

        // 7) 초대 정보 생성 (inviteId 유지)
        Long inviteId = party.getPartyId() * 1_000_000 + friendId;

        LobbyInvitePayload payload = LobbyInvitePayload.builder()
                .inviteId(inviteId)
                .hostId(hostId)
                .hostNickname(hostNickname)
                .partyId(party.getPartyId())
                .build();

        // 8) NotificationDto 생성
        NotificationDto notification = NotificationDto.builder()
                .type(NotificationType.LOBBY_INVITE)
                .senderId(hostId)
                .senderNickname(hostNickname)
                .message(hostNickname + " 님이 파티에 초대했습니다.")
                .data(payload)
                .build();

        // 9) 초대받는 유저에게 SSE 알림 전송
        notificationService.notify(friendId, notification);

        return ResponseEntity.ok("초대 전송 완료");
    }
}
