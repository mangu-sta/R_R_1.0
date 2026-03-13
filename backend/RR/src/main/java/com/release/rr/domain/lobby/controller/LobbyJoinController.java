package com.release.rr.domain.lobby.controller;

import com.release.rr.domain.characters.dto.CharacterResponseDto;
import com.release.rr.domain.characters.service.CharacterService;
import com.release.rr.domain.lobby.dto.*;
import com.release.rr.domain.lobby.model.LobbyParty;
import com.release.rr.domain.lobby.service.LobbyPartyService;
import com.release.rr.global.exception.CustomException;
import com.release.rr.global.exception.ErrorCode;
import com.release.rr.global.redis.dao.RedisGameRoomDao;
import com.release.rr.global.security.SecurityUtil;
import io.jsonwebtoken.Claims;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 대기실(파티) 입장 관련 API 컨트롤러
 * - /join → 파티 직접 입장 (초대 없이 참여하는 경우)
 * - /invite/accept → 초대 수락 후 파티 참여
 */
@RestController
@RequestMapping(value = "/api/lobby")
@RequiredArgsConstructor
public class LobbyJoinController {

    private final LobbyPartyService lobbyPartyService;
    private final CharacterService characterService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final RedisGameRoomDao  redisGameRoomDao;

    // ==========================================================
    // 🔥 1) 파티 직접 입장 API (초대 없이 바로 입장)
    // ==========================================================
    @PostMapping(value = "/join")
    public ResponseEntity<?> joinParty(@RequestBody JoinRequest request) {

        // JWT에서 로그인한 사용자 정보 가져오기
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // 요청으로 받은 partyId 기준으로 파티 참여
        try {
            lobbyPartyService.joinParty(request.getPartyId(), userId);
        } catch (IllegalStateException e) {
            throw new CustomException(ErrorCode.PARTY_JOIN_FAILED);   // ⭐ 변경
        }

        // 🔥 파티 정보 로드
        LobbyParty party = lobbyPartyService.findByPartyId(request.getPartyId());
        if (party == null) {
            throw new CustomException(ErrorCode.PARTY_NOT_FOUND);      // ⭐ 추가
        }

        // 🔥 파티 멤버 캐릭터 정보 로드
        List<CharacterResponseDto> members = party.getMemberIds().stream()
                .map(characterService::getCharacterByUserId)
                .map(CharacterResponseDto::from)
                .toList();

        // 🔥 roomId 포함한 파티 정보 반환
        LobbyPartyResponseDto response = LobbyPartyResponseDto.builder()
                .partyId(party.getPartyId())
                .hostId(party.getHostId())
                .roomId(party.getRoomId())
                .members(members)
                .build();

        return ResponseEntity.ok(response);
    }

    @Data
    public static class JoinRequest {
        private Long partyId;   // 참여하려는 파티 ID
        private Long inviteId;  // 초대 기반 참여 시 검증용(선택)
    }

    // ==========================================================
    // 🔥 2) 초대 수락 API
    // ==========================================================
    @PostMapping("/invite/accept")
    public ResponseEntity<?> acceptInvite(@RequestBody InviteAcceptRequestDto requestDto) {

     /*   Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Claims claims)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        Long userId = claims.get("userId", Long.class);
        Long partyId = requestDto.getPartyId();
        */

        // ✅ 1) 인증 통일 (유저 식별만 SecurityUtil)
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // ✅ 2) partyId는 기존 그대로
        Long partyId = requestDto.getPartyId();


        // ⭐ 초대받기 전 사용자가 구독 중이던 roomId 저장
        LobbyParty myOldParty = lobbyPartyService.findPartyByUserId(userId);
        String oldRoomId = (myOldParty != null) ? myOldParty.getRoomId() : null;

        LobbyParty targetParty = lobbyPartyService.findByPartyId(partyId);
        if (targetParty == null) {
            throw new CustomException(ErrorCode.PARTY_NOT_FOUND);
        }

        // ⭐ 기존 파티에서 나가기
        if (myOldParty != null && !myOldParty.getPartyId().equals(partyId)) {
            lobbyPartyService.leaveParty(myOldParty.getPartyId(), userId);
        }

        // ⭐ 파티 참여 처리
        lobbyPartyService.joinParty(partyId, userId);

        // ⭐ join 후 새 파티 조회
        LobbyParty newParty = lobbyPartyService.findPartyByUserId(userId);
        if (newParty == null) {
            throw new CustomException(ErrorCode.PARTY_NOT_FOUND);
        }

        List<CharacterResponseDto> members = newParty.getMemberIds().stream()
                .map(characterService::getCharacterByUserId)
                .map(CharacterResponseDto::from)
                .toList();

        LobbyPartyResponseDto response = LobbyPartyResponseDto.builder()
                .partyId(newParty.getPartyId())
                .hostId(newParty.getHostId())
                .roomId(newParty.getRoomId())
                .members(members)
                .build();

        String newRoomId = newParty.getRoomId();

        // ==========================================================
        // ⭐ 1) 새로운 방(topic)에 전체 broadcast
        // ==========================================================
        simpMessagingTemplate.convertAndSend(
                "/topic/lobby/" + newRoomId,
                new LobbyEventMessage(
                        LobbyEventType.ACCEPT,
                        new LobbyUpdateValue("ACCEPT")
                )
        );

        // ==========================================================
        // ⭐ 2) 초대받은 본인이 기존에 구독하던 방에도 broadcast
        //     → 그래야 본인의 UI가 즉시 갱신됨
        // ==========================================================
        if (oldRoomId != null && !oldRoomId.equals(newRoomId)) {
            simpMessagingTemplate.convertAndSend(
                    "/topic/lobby/" + oldRoomId,
                    new LobbyEventMessage(
                            LobbyEventType.ACCEPT,
                            new LobbyUpdateValue("ACCEPT")
                    )
            );
        }

        return ResponseEntity.ok(response);
    }

}
