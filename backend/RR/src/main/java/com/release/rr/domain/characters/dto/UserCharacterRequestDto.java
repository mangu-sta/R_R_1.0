package com.release.rr.domain.characters.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCharacterRequestDto {
    private Long userId;   // null 가능 → null이면 본인
}
