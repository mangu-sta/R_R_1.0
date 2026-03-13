package com.release.rr.domain.user.dto;


import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SignInRequestDto {
    private String nickname;
    private String password;
}

