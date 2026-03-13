package com.release.rr.domain.lobby.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StartGameConfirmRequest {

    // ACCEPT / REJECT
    private StartConfirmResponse response;
}
