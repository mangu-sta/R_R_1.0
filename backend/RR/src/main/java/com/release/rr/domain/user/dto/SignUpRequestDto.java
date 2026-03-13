package com.release.rr.domain.user.dto;


import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SignUpRequestDto {
    private String nickname;
    private String password;
}