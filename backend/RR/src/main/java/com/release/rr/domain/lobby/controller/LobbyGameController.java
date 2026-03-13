package com.release.rr.domain.lobby.controller;

import com.release.rr.domain.lobby.dto.StartGameConfirmRequest;
import com.release.rr.domain.lobby.service.LobbyGameService;
import com.release.rr.global.security.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/lobby")
public class LobbyGameController {

    private final LobbyGameService lobbyGameService;


    @PostMapping("/{mapNanoId}/start-request")
    public ResponseEntity<?> requestStartGame(
            @PathVariable String mapNanoId
    ) {
        System.out.println(
                "🔥 CONTROLLER HIT pid=" + ProcessHandle.current().pid()
                        + " nanoId=" + mapNanoId
        );

        Long userId = SecurityUtil.getCurrentUserId();
        lobbyGameService.requestStartGame(mapNanoId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{nanoId}/start-response")
    public ResponseEntity<Void> respondStartGame(
            @PathVariable String nanoId,
            @RequestBody StartGameConfirmRequest req
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        lobbyGameService.confirmStartGame(nanoId, userId, req.getResponse());
        return ResponseEntity.ok().build();
    }

}

