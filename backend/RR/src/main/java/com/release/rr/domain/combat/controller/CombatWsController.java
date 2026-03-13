package com.release.rr.domain.combat.controller;

import com.release.rr.domain.combat.dto.req.MonsterHitRequest;
import com.release.rr.domain.combat.service.CombatFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping(value = "/api")

public class CombatWsController {

    private final CombatFacadeService combatFacadeService;

    @MessageMapping("/game/{nanoId}/monster-hit")
    public void monsterHit(
            @DestinationVariable String nanoId,
            @Payload MonsterHitRequest req
    ) {
        combatFacadeService.handleMonsterHit(nanoId, req);
    }

    @MessageMapping("/game/{nanoId}/stat-select")
    public void statSelect(
            @DestinationVariable String nanoId,
            @Payload com.release.rr.domain.combat.dto.req.StatSelectionRequest req
    ) {
        combatFacadeService.handleStatSelection(nanoId, req);
    }
}
