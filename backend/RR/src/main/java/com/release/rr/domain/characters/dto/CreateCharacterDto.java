package com.release.rr.domain.characters.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class CreateCharacterDto {

    private Long userId;     // 어떤 유저가 생성하는지
    private String job;      // FIREFIGHTER / SOLDIER / DOCTOR / REPORTER
}
