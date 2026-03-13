package com.release.rr.domain.lobby.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LobbyUserDto {
    private Long userId;
    private String nickname;
    private Integer level;
}
