package com.release.rr.domain.lobby.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LobbyChatMessageDto {

    private String nickname;
    private String message;
    private LocalDateTime sentAt;
}
