package com.release.rr.global.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // ========================
    // 인증 / 권한
    // ========================
    LOGIN_FAILED(401, "AUTH_001", "아이디 또는 비밀번호가 일치하지 않습니다."),
    UNAUTHORIZED(401, "AUTH_002", "로그인이 필요합니다."),

    // ========================
    // 회원가입 관련
    // ========================
    DUPLICATE_NICKNAME(400, "USER_001", "이미 존재하는 닉네임입니다."),
    INVALID_NICKNAME(400, "USER_002", "닉네임 형식이 올바르지 않습니다."),
    INVALID_PASSWORD(400, "USER_003", "비밀번호 형식이 올바르지 않습니다."),

    // ========================
    // 유저
    // ========================
    USER_NOT_FOUND(404, "USER_004", "유저 정보를 찾을 수 없습니다."),

    // ========================
    // 캐릭터
    // ========================
    CHARACTER_NOT_FOUND(404, "CHAR_001", "캐릭터가 존재하지 않습니다."),
    CHARACTER_ALREADY_EXISTS(400, "CHAR_002", "이미 생성된 캐릭터가 있습니다."),
    CHARACTER_STATE_NOT_INITIALIZED(
            409,
            "CHAR_003",
            "캐릭터 상태가 아직 초기화되지 않았습니다."
    ),

    // ========================
    // 맵
    // ========================
    MAP_NOT_FOUND(404, "MAP_001", "맵 정보를 찾을 수 없습니다."),

    // ========================
    // 친구 시스템
    // ========================

    FRIEND_REQUEST_ALREADY_SENT(400, "FRIEND_001", "이미 친구 요청을 보냈습니다."),
    FRIEND_ALREADY(400, "FRIEND_002", "이미 친구 상태입니다."),
    FRIEND_REQUEST_NOT_FOUND(404, "FRIEND_003", "친구 요청을 찾을 수 없습니다."),
    CANNOT_ADD_SELF(400, "FRIEND_004", "자기 자신에게는 친구 요청을 보낼 수 없습니다."),
    FRIEND_NOT_FOUND(404, "FRIEND_005", "친구 정보를 찾을 수 없습니다."),
    FRIEND_ALREADY_BLOCKED(400, "FRIEND_006", "이미 차단한 사용자입니다."),
    FRIEND_NOT_BLOCKED(400, "FRIEND_007", "차단 상태가 아닙니다."),


    // ========================
    // 파티 초대 및 참가 관련
    // ========================
    INVITE_TARGET_NOT_FOUND(404, "LOBBY_INVITE_001", "해당 닉네임의 유저를 찾을 수 없습니다."),
    INVITE_SELF_NOT_ALLOWED(400, "LOBBY_INVITE_002", "자기 자신에게 초대할 수 없습니다."),
    INVITE_NOT_HOST(400, "LOBBY_INVITE_003", "방장만 초대할 수 있습니다."),
    INVITE_PARTY_FULL(400, "LOBBY_INVITE_004", "파티 인원이 가득 찼습니다."),
    INVITE_ALREADY_IN_PARTY(400, "LOBBY_INVITE_005", "해당 유저는 이미 파티에 속해 있습니다."),
    INVITE_ALREADY_MEMBER(400, "LOBBY_INVITE_006", "이미 파티에 있는 유저입니다."),
    PARTY_NOT_FOUND(404, "LOBBY_001", "해당 파티가 존재하지 않습니다."),
    PARTY_JOIN_FAILED(400, "LOBBY_002", "파티에 참가할 수 없습니다."),
    PARTY_ALREADY_IN(400, "LOBBY_003", "이미 다른 파티에 속해 있습니다."),



    // ========================
    // 파티 / 대기실
    // ========================
    LOBBY_NOT_FOUND(404, "LOBBY_001", "현재 속한 파티가 없습니다."),
    NOT_HOST(400, "LOBBY_002", "방장만 수행할 수 있는 작업입니다."),
    TARGET_USER_NOT_FOUND(404, "LOBBY_003", "대상 유저를 찾을 수 없습니다."),
    TARGET_NOT_IN_PARTY(400, "LOBBY_004", "해당 유저는 파티에 속해 있지 않습니다."),
    CANNOT_KICK_SELF(400, "LOBBY_005", "자기 자신을 추방할 수 없습니다."),
    NOT_IN_PARTY(400, "LOBBY_006", "현재 파티에 속해 있지 않습니다."),

    // ========================
    // 게임 / 인게임
    // ========================
    NO_PLAYERS(400, "GAME_001", "게임에 참여할 플레이어가 없습니다."),
    GAME_ALREADY_STARTED(400, "GAME_002", "이미 게임이 시작된 상태입니다."),
    INVALID_GAME_ROOM(400, "GAME_003", "유효하지 않은 게임 룸입니다."),
    PLAYER_NOT_IN_GAME_ROOM(403, "GAME_004", "해당 게임 룸의 참가자가 아닙니다."),
    GAME_RECONNECT_EXPIRED(403, "GAME_005", "재접속 가능 시간이 초과되었습니다."),
    GAME_START_ALREADY_IN_PROGRESS(409, "GAME_006", "이미 게임 시작 수락이 진행 중입니다."),


    // ========================
    // WebSocket / 실시간
    // ========================
    WEBSOCKET_SESSION_INVALID(500, "WS_001", "WebSocket 세션 인증 정보가 유실되었습니다."),


    // ========================
    // 공통 서버 오류
    // ========================
    INTERNAL_SERVER_ERROR(500, "SERVER_001", "서버 내부 오류가 발생했습니다.");

    private final int status;
    private final String code;
    private final String message;

    ErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
