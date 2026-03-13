package com.release.rr.global.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());      // 예외 메시지는 ErrorCode의 메시지로 유지
        this.errorCode = errorCode;
    }
}
