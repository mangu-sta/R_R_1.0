package com.release.rr.domain.monster.ai;

public enum MonsterAiState {
    IDLE,     // 타겟 없음
    CHASE,    // 타겟 추적
    ATTACK,   // 공격 범위
    PATTERN,  // 특수 패턴 (보스 등)
    DEAD,

}
