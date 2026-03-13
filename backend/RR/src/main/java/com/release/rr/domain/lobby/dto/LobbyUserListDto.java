package com.release.rr.domain.lobby.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LobbyUserListDto {
    private List<LobbyUserDto> users;
}
