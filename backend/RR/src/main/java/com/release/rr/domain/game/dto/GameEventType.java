package com.release.rr.domain.game.dto;

public enum GameEventType {
    MOVE,
    HP,
    FIRE,
    MONSTER_HP,
    ANGLE,
    ROTATE,

    // ===== 몬스터 =====
    MONSTER_STATE,
    MONSTER_EVENT,

    // ===== 플레이어 =====
    PLAYER_STATE,
    PLAYER_EVENT,
    PLAYER_HP_UPDATE,
    // ===== 시스템 =====
    GAME_START,
    GAME_END,
    GAME_SAVE_SUCCESS,
    GAME_SAVE_FAIL,

    //====
    BULLET
}
