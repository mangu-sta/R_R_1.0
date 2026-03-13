package com.release.rr.domain.user.dto.Quarantine;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginErrorResponseDto {
    private boolean success;
    private String message;
}
