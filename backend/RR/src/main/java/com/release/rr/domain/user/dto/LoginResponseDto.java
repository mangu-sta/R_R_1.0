package com.release.rr.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDto {
    private boolean success;
    private boolean hasCharacter;
}
