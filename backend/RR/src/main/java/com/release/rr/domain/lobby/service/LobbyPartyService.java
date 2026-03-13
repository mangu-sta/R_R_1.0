package com.release.rr.domain.lobby.service;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.release.rr.domain.lobby.dto.LobbyStatus;
import com.release.rr.domain.lobby.dto.StartConfirmResponse;
import com.release.rr.domain.lobby.event.GameAutoRestartEvent;
import com.release.rr.domain.lobby.model.LobbyParty;
import com.release.rr.domain.map.entity.MapEntity;
import com.release.rr.domain.map.repository.MapRepository;
import com.release.rr.domain.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class LobbyPartyService {

    private final MapRepository mapRepository;
    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    // 현재 서버에 존재하는 모든 파티 목록을 저장하는 Map
    // key = partyId(현재는 hostId와 동일), value = LobbyParty 객체
    private final Map<Long, LobbyParty> parties = new ConcurrentHashMap<>();
    /**
     * 파티 생성 또는 기존 파티 조회
     *
     * - hostId(파티장의 userId)를 기준으로 파티를 생성하거나 기존 파티를 반환한다.
     * - 초대 기능에서 새로운 파티를 자동으로 만들 때 사용됨.
     * - computeIfAbsent() 덕분에 동시성 환경에서도 안전함.
     */

    // nanoId 기준 게임 시작 수락 상태
    private final Map<String, Map<Long, StartConfirmResponse>> startConfirmMap
            = new ConcurrentHashMap<>();

    // nanoId 기준 로비 상태
    private final Map<String, LobbyStatus> lobbyStatusMap
            = new ConcurrentHashMap<>();

    public LobbyParty getOrCreateParty(Long hostId) {

        // 1) map 조회, 없으면 생성
        MapEntity map = mapRepository.findByOwnerUserId(hostId)
                .orElseGet(() -> {
                    MapEntity newMap = MapEntity.builder()
                            .owner(UserEntity.builder().userId(hostId).build())
                            .nanoId(NanoIdUtils.randomNanoId())
                            .mapName("Lobby-" + hostId)
                            .build();
                    return mapRepository.save(newMap);
                });

        String roomId = map.getNanoId();  // 🔥 고정된 nanoId 사용

        // 2) party 생성
        return parties.computeIfAbsent(hostId,
                id -> LobbyParty.create(hostId, roomId));
    }


    /**
     * 파티 ID로 파티 정보를 조회
     *
     * - 특정 partyId에 해당하는 LobbyParty를 반환한다.
     * - 초대 수락, 대기실 조회 등 파티를 직접 찾아야 할 때 사용.
     */
    public LobbyParty findByPartyId(Long partyId) {
        return parties.get(partyId);
    }

    /**
     * 파티 제거 (파티가 해체되었을 때)
     *
     * - 모든 멤버가 나갔거나 방장이 파티를 종료시킬 때 사용한다.
     */
    public void removeParty(Long partyId) {
        parties.remove(partyId);
    }

    /**
     * 특정 파티(partyId)에 특정 유저(userId)가 포함되어 있는지 여부 체크
     *
     * - “이 유저가 이 파티에 속해있는가?” 를 판단할 때 사용.
     * - 초대/수락/나가기 등의 검증에 필요.
     */
    public boolean isInParty(Long partyId, Long userId) {
        LobbyParty party = parties.get(partyId);
        return party != null && party.getMemberIds().contains(userId);
    }

    /**
     * 🔥 유저가 어떤 파티에 속해 있는지 전체 파티에서 검색
     *
     * - friendId가 초대받기 전에 이미 다른 파티에 소속되어 있는지 확인할 때 사용.
     * - 유저는 동시에 두 개 이상의 파티에 속할 수 없기 때문에 필수 검증임.
     */
    public boolean isUserInAnyParty(Long userId) {
        return parties.values().stream()
                .anyMatch(p -> p.getMemberIds().contains(userId));
    }

    /**
     * 🔥 로그인한 유저가 속한 파티를 찾아 반환
     *
     * - /api/lobby/me (대기실 화면 불러오기) API에서 반드시 필요.
     * - 초대 수락 후 자신이 들어간 파티 정보를 가져올 때도 사용.
     */
    public LobbyParty findPartyByUserId(Long userId) {
        return parties.values().stream()
                .filter(p -> p.getMemberIds().contains(userId))
                .findFirst()
                .orElse(null);
    }

    /**
     * nanoId(=roomId) 기준으로 파티 조회
     *
     * - 게임 시작, 수락 완료 후 실제 게임 시작 단계에서 사용
     * - hostId와 무관하게 "현재 로비(방)" 자체를 식별하기 위한 메서드
     */
    public LobbyParty findPartyByNanoId(String nanoId) {
        return parties.values().stream()
                .filter(party -> nanoId.equals(party.getRoomId()))
                .findFirst()
                .orElse(null);
    }


    /**
     * 파티 참여(join)
     *
     * - 초대 수락 시 호출되는 핵심 기능.
     * - partyId 기준으로 해당 LobbyParty 객체를 찾아 해당 유저를 멤버 목록에 추가한다.
     * - 파티가 가득 찼다면 예외를 던진다.
     */
    public void joinParty(Long partyId, Long userId) {
        LobbyParty party = parties.get(partyId);
        if (party == null) {
            throw new IllegalStateException("파티가 존재하지 않습니다.");
        }
        if (party.isFull()) {
            throw new IllegalStateException("파티 인원이 가득 찼습니다.");
        }
        party.addMember(userId);
    }

    /**
     * (선택 기능) 파티 나가기
     *
     * - userId를 파티에서 제거한다.
     * - 제거 후 파티에 멤버가 0명이라면 해당 파티를 폐기한다.
     * - 게임 종료, 로그아웃, 수동 나가기 등에서 사용.
     */
    public void leaveParty(Long partyId, Long userId) {
        LobbyParty party = parties.get(partyId);
        if (party == null) return;

      //  String nanoId = party.getRoomId();

    /*// 🔥 수락 도중 나가기 처리
        if (lobbyStatusMap.get(nanoId) == LobbyStatus.CONFIRMING) {
            resetConfirm(nanoId);

            // 여기서는 Service라서 브로드캐스트는 위임
            // 또는 이벤트 리턴
        }*/

        boolean wasHost = party.getHostId().equals(userId);

        party.removeMember(userId);

        // 파티 비어있으면 삭제
        if (party.getMemberIds().isEmpty()) {
            parties.remove(partyId);
            return;
        }

        // 방장 나감 → 새 호스트 지정
        if (wasHost) {
            // 1️⃣ 새 호스트 = LinkedHashSet의 0번
            Long newHost = party.getMemberIds().iterator().next();
            party.setHostId(newHost);

            // 2️⃣ 새 호스트의 개인 맵 nanoId 조회
            MapEntity newHostMap = mapRepository.findByOwnerUserId(newHost)
                    .orElseThrow(() -> new IllegalStateException("새 호스트의 맵이 없습니다."));

            String newRoomId = newHostMap.getNanoId();
            String oldRoomId = party.getRoomId();

            // 3️⃣ roomId 교체 (🔥 핵심)
            party.setRoomId(newRoomId);

            // 4️⃣ parties Map key 교체
            parties.remove(partyId);
            parties.put(newHost, party);

            // 6️⃣ 새 방 상태 초기화
            lobbyStatusMap.put(newRoomId, LobbyStatus.WAITING);

            // ✅ 자동 재시작 트리거
            boolean shouldAutoRestart =
                    lobbyStatusMap.get(oldRoomId) == LobbyStatus.IN_GAME;

            // 5️⃣ 이전 방 상태 정리
            lobbyStatusMap.remove(oldRoomId);
            startConfirmMap.remove(oldRoomId);

            if (shouldAutoRestart) {
                // ⚠️ 바로 실행하면 트랜잭션 꼬일 수 있음
                // 이벤트 or 비동기 권장
                // 트랜잭션 끝난 뒤 실행
                eventPublisher.publishEvent(
                        new GameAutoRestartEvent(newRoomId)
                );
            }

        }


        // 개인 로비 세팅 (파티와 완전히 분리)
        setPersonalLobby(userId);

    }


    /**
     * 로비 상태를 "게임 시작 수락 대기(CONFIRMING)" 상태로 변경
     * - 게임 시작 요청이 들어왔을 때 최초 1회 호출됨
     */
    public void markConfirming(String nanoId) {
        lobbyStatusMap.put(nanoId, LobbyStatus.CONFIRMING);
    }

    /**
     * 게임 시작 수락 상태 초기화
     * - 로비에 속한 모든 유저를 PENDING 상태로 설정
     * - 이후 각 유저의 수락/거절 응답을 기록하기 위한 준비 단계
     */
    public void initStartConfirm(String nanoId, Iterable<Long> userIds) {
        Map<Long, StartConfirmResponse> map = new ConcurrentHashMap<>();
        for (Long userId : userIds) {
            map.put(userId, StartConfirmResponse.PENDING);
        }
        startConfirmMap.put(nanoId, map);
    }

    /**
     * 특정 유저의 게임 시작 수락/거절 응답을 기록
     * - ACCEPT / REJECT 중 하나로 상태가 갱신됨
     */
    public void recordStartConfirm(
            String nanoId,
            Long userId,
            StartConfirmResponse response
    ) {
        Map<Long, StartConfirmResponse> map = startConfirmMap.get(nanoId);
        if (map == null) {
            throw new IllegalStateException("수락 상태가 초기화되지 않았습니다.");
        }
        map.put(userId, response);
    }

    /**
     * 게임 시작 수락 상태를 초기화하고 로비 상태를 다시 WAITING으로 되돌림
     * - 누군가 거절했거나, 수락 과정이 중단되었을 때 사용
     */
    public void resetConfirm(String nanoId) {
        startConfirmMap.remove(nanoId);
        lobbyStatusMap.put(nanoId, LobbyStatus.WAITING);
    }

    /**
     * 로비에 속한 모든 유저가 게임 시작을 수락했는지 여부 확인
     * - 하나라도 PENDING 또는 REJECT가 있으면 false
     * - 전원 ACCEPT일 때만 true 반환
     */
    public boolean isAllAccepted(String nanoId) {
        Map<Long, StartConfirmResponse> map = startConfirmMap.get(nanoId);
        if (map == null) return false;

        return map.values().stream()
                .allMatch(v -> v == StartConfirmResponse.ACCEPT);
    }

    /**
     * 현재 로비 상태 조회
     *
     * - 기본값은 WAITING
     * - CONFIRMING / IN_GAME 등 상태 판단에 사용
     */
    public LobbyStatus getLobbyStatus(String nanoId) {
        return lobbyStatusMap.getOrDefault(nanoId, LobbyStatus.WAITING);
    }


    /**
     * 중복 수락 방지 -
     * */
    public StartConfirmResponse getConfirmStatus(String nanoId, Long userId) {
        Map<Long, StartConfirmResponse> map = startConfirmMap.get(nanoId);
        return map == null ? null : map.get(userId);
    }
    // 게임 시작 후 상태 전환 용도
    public void markInGame(String nanoId) {
        lobbyStatusMap.put(nanoId, LobbyStatus.IN_GAME);
    }


    private static final String USER_LOBBY_KEY = "USER:LOBBY:";

    public void setPersonalLobby(Long userId) {
        MapEntity map = mapRepository.findByOwnerUserId(userId)
                .orElseThrow(() -> new IllegalStateException("개인 맵이 없습니다."));

        redisTemplate.opsForValue().set(
                USER_LOBBY_KEY + userId,
                map.getNanoId()
        );
    }

    public String getPersonalLobbyRoomId(Long userId) {
        return redisTemplate.opsForValue().get(USER_LOBBY_KEY + userId);
    }


    public boolean wasInGame(String nanoId) {
        return lobbyStatusMap.get(nanoId) == LobbyStatus.IN_GAME;
    }



}
