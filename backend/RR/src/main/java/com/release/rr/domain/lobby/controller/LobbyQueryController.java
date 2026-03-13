package com.release.rr.domain.lobby.controller;

import com.release.rr.domain.characters.dto.CharacterResponseDto;
import com.release.rr.domain.lobby.dto.LobbyPartyResponseDto;
import com.release.rr.domain.lobby.model.LobbyParty;
import com.release.rr.domain.lobby.service.CharacterQueryService;
import com.release.rr.domain.lobby.service.LobbyPartyService;
import com.release.rr.domain.map.repository.MapRepository;
import com.release.rr.domain.user.service.UserAuthService;
import com.release.rr.global.exception.CustomException;
import com.release.rr.global.exception.ErrorCode;
import com.release.rr.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/api/lobby")
@RequiredArgsConstructor
public class LobbyQueryController {

    private final LobbyPartyService lobbyPartyService;
    private final CharacterQueryService characterQueryService;
    private final UserAuthService  userAuthService;
    private final com.release.rr.global.redis.dao.GameProgressRedisDao gameProgressRedisDao;
    private final MapRepository  mapRepository;
    // 내 파티 조회 API
    @PostMapping("/me")
    public ResponseEntity<LobbyPartyResponseDto> getMyParty() {

        // JWT claims
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        CharacterResponseDto me = characterQueryService.getCharacterWithCurrentHp(userId);
        System.out.println("[LobbyQueryController] getMyParty userId=" + userId + ", nickname=" + me.getNickname() + ", job=" + me.getJob());
        // 현재 속한 파티 조회
        LobbyParty party = lobbyPartyService.findPartyByUserId(userId);

        // ⭐ 없으면 새로 생성
        if (party == null) {
            party = lobbyPartyService.getOrCreateParty(userId);
        }

        // 멤버 캐릭터 정보 조회 (캐릭터 없는 유저가 있을 경우 예외 처리됨)
        List<CharacterResponseDto> members =
                party.getMemberIds().stream()
                        .map(characterQueryService::getCharacterWithCurrentHp)
                        .toList();

        members.forEach(m ->
                log.info("LOBBY ME HP userId={} hp={}", m.getUserId(), m.getHp())
        );


        // ⭐ 내 캐릭터 정보가 반드시 있어야 한다면 예외 발생
        CharacterResponseDto myCharacter = members.stream()
                .filter(c -> c.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND));  // ⭐ 변경 가능

        // ⭐ 키 설정 조회
        String keySetting = userAuthService.getKeySetting(userId);
        // int stage = mapRepository.findStageByNanoId(party.getRoomId()); // DB Query -> Redis Query
        int stage = gameProgressRedisDao.getStage(party.getRoomId());

        LobbyPartyResponseDto response = LobbyPartyResponseDto.builder()
                .partyId(party.getPartyId()) // 파티 ID
                .hostId(party.getHostId())   // 방=장
                .roomId(party.getRoomId())   // 대기실 아이디
                .myCharacter(myCharacter)    // 본인 캐릭터
                .members(members)            // 파티원 캐릭터
                .keySetting(keySetting)      // 키 세팅
                .stage(stage)
                .build();

        return ResponseEntity.ok(response);
    }

}
