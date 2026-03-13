package com.release.rr.domain.combat.dto.req;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StatSelectionRequest {
    private Long userId;
    private int statIndex; // 1: Strength, 2: Agility, 3: Health, 4: Reload
}
