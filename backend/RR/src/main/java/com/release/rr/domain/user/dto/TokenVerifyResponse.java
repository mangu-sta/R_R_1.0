package com.release.rr.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenVerifyResponse {
    private boolean valid;
    private Long userId;  // 유효하지 않으면 null
}
