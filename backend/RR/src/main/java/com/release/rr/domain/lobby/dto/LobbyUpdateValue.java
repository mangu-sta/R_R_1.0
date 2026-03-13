package com.release.rr.domain.lobby.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LobbyUpdateValue {
    private String message;  // "UPDATED", "KICKED", "LEFT" 등
}
