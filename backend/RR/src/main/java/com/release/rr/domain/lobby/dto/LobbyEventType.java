package com.release.rr.domain.lobby.dto;

public enum LobbyEventType {
    KICK,       // 추방
    LEAVE,      // 본인 나가기
    ACCEPT,     // 초대 수락
    MESSAGE,     // 일반 메시지
    GAME_START,
    GAME_START_REJECTED,
    GAME_START_REQUEST,
    GAME_START_ACCEPTED,
    HOST_LEFT,   //호스트 나감\
    STAGE_ADVANCE_REQUEST, //인게임 스테이지 용
    STAGE_ADVANCE_ACCEPTED,//
    STAGE_ADVANCE_REJECTED, //
    STAGE_ADVANCE_COMPLETED //
}
