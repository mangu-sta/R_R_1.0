package com.release.rr.domain.map.dto;

import lombok.Getter;

@Getter
public class PlayerRotateRequestDto {
    private Long userId;
    private float angle;
    private long timestamp;
}

