package com.release.rr.domain.lobby.dto;

import lombok.Data;

@Data
public class LobbyChatRequestDto {
    private String nickname; //닉네임
    private String message; // 채팅 내용
}
