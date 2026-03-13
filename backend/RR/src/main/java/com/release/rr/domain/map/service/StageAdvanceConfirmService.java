package com.release.rr.domain.map.service;

import com.release.rr.domain.lobby.dto.LobbyEventMessage;
import com.release.rr.domain.lobby.dto.LobbyEventType;
import com.release.rr.domain.lobby.dto.StartConfirmResponse;
import com.release.rr.domain.lobby.model.LobbyParty;
import com.release.rr.domain.lobby.service.LobbyPartyService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StageAdvanceConfirmService {

    private final LobbyPartyService lobbyPartyService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MapStageTransitionService mapStageTransitionService;

    @Transactional
    public void requestStageAdvance(String nanoId, Long requesterId) {

        LobbyParty party = lobbyPartyService.findPartyByUserId(requesterId);
        if (party == null) {
            throw new IllegalStateException("파티가 없습니다.");
        }

        // 1️⃣ 로비 상태 CONFIRMING
        lobbyPartyService.markConfirming(nanoId);

        // 2️⃣ 모든 멤버 PENDING
        lobbyPartyService.initStartConfirm(nanoId, party.getMemberIds());

        // 3️⃣ 전원에게 Stage 전환 수락 요청 브로드캐스트
        messagingTemplate.convertAndSend(
                "/topic/game/" + nanoId,
                new LobbyEventMessage(
                        LobbyEventType.STAGE_ADVANCE_REQUEST,
                        requesterId
                )
        );
    }


    @Transactional
    public void confirmStageAdvance(
            String nanoId,
            Long userId,
            StartConfirmResponse response
    ) {
        LobbyParty party = lobbyPartyService.findPartyByNanoId(nanoId);
        if (party == null || !party.getMemberIds().contains(userId)) {
            throw new IllegalStateException("권한 없음");
        }

        // 중복 방지
        StartConfirmResponse prev =
                lobbyPartyService.getConfirmStatus(nanoId, userId);
        if (prev != StartConfirmResponse.PENDING) return;

        lobbyPartyService.recordStartConfirm(nanoId, userId, response);

        // UX용 수락 브로드캐스트
        if (response == StartConfirmResponse.ACCEPT) {
            messagingTemplate.convertAndSend(
                    "/topic/game/" + nanoId,
                    new LobbyEventMessage(
                            LobbyEventType.STAGE_ADVANCE_ACCEPTED,
                            userId
                    )
            );
        }

        // 거절 즉시 종료
        if (response == StartConfirmResponse.REJECT) {
            lobbyPartyService.resetConfirm(nanoId);
            messagingTemplate.convertAndSend(
                    "/topic/game/" + nanoId,
                    new LobbyEventMessage(
                            LobbyEventType.STAGE_ADVANCE_REJECTED,
                            userId
                    )
            );
            return;
        }

        // 🔥 전원 수락 시
        if (!lobbyPartyService.isAllAccepted(nanoId)) {
            return;
        }

        // 1️⃣ 실제 Stage 전환
        mapStageTransitionService.advanceStage0To1(nanoId);

        // 2️⃣ 🔥🔥🔥 전원에게 Stage 전환 완료 알림 (이게 빠져 있었음!)
        messagingTemplate.convertAndSend(
                "/topic/game/" + nanoId,
                new LobbyEventMessage(
                        LobbyEventType.STAGE_ADVANCE_COMPLETED,
                        1   // 또는 new StageChangePayload(0, 1)
                )
        );
    }


}

