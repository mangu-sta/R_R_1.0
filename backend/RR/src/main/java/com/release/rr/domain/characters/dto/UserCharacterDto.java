package com.release.rr.domain.characters.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCharacterDto {

    private String nickname;  // 유저 닉네임
    private CharacterResponseDto character; // 기존 캐릭터 DTO

    public UserCharacterDto(String nickname, CharacterResponseDto character) {
        this.nickname = nickname;
        this.character = character;
    }

    public static UserCharacterDto of(String nickname, CharacterResponseDto dto) {
        return new UserCharacterDto(nickname, dto);
    }
}
