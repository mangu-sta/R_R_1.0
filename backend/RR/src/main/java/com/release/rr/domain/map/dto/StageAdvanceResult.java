package com.release.rr.domain.map.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StageAdvanceResult {

    private String nanoId;
    private int prevStage;
    private int currentStage;
    private String message;

    public static StageAdvanceResult success(
            String nanoId, int prev, int current
    ) {
        return new StageAdvanceResult(nanoId, prev, current, "OK");
    }

    public static StageAdvanceResult alreadyAdvanced(
            String nanoId, int current
    ) {
        return new StageAdvanceResult(
                nanoId, current, current, "Stage already advanced"
        );
    }
}
