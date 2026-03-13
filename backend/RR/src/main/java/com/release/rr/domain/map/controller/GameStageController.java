package com.release.rr.domain.map.controller;

import com.release.rr.domain.lobby.dto.StartGameConfirmRequest;

import com.release.rr.domain.map.service.StageAdvanceConfirmService;
import com.release.rr.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameStageController {

    private final StageAdvanceConfirmService stageAdvanceConfirmService;

    @PostMapping("/{nanoId}/stage-advance/request")
    public ResponseEntity<Void> requestStageAdvance(
            @PathVariable String nanoId
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        stageAdvanceConfirmService.requestStageAdvance(nanoId, userId);
        return ResponseEntity.ok().build();
    }



    @PostMapping("/{nanoId}/stage-advance/response")
    public ResponseEntity<Void> respondStageAdvance(
            @PathVariable String nanoId,
            @RequestBody StartGameConfirmRequest req
    ) {
        Long userId = SecurityUtil.getCurrentUserId();
        stageAdvanceConfirmService.confirmStageAdvance(
                nanoId, userId, req.getResponse()
        );
        return ResponseEntity.ok().build();
    }

}

