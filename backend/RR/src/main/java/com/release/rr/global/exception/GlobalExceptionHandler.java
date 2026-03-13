package com.release.rr.global.exception;


import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<?> handleCustomException(
            CustomException e,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        // ✅ WebSocket/SockJS 요청은 건드리지 않음
        if (isSpecialRequest(request)) {
            return ResponseEntity.status(e.getErrorCode().getStatus()).build();
        }

        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(Map.of(
                        "success", false,
                        "errorCode", e.getErrorCode().getCode(),
                        "message", e.getErrorCode().getMessage()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(
            Exception e,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        e.printStackTrace();

        if (isSpecialRequest(request)) {
            // ✅ SockJS/WebSocket은 응답 바디 없이 종료
            return ResponseEntity.status(500).build();
        }

        ErrorCode code = ErrorCode.INTERNAL_SERVER_ERROR;

        return ResponseEntity
                .status(code.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "success", false,
                        "errorCode", code.getCode(),
                        "message", code.getMessage()
                ));

    }

 /*   private boolean isWebSocketRequest(jakarta.servlet.http.HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith("/ws");
    }*/

    private boolean isSpecialRequest(jakarta.servlet.http.HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) return false;
        // ✅ 기존 /ws 체크에 /notifications(알림 구독) 경로를 추가합니다.
        return uri.startsWith("/ws") || uri.contains("/notifications");
    }


    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<?> handleAsyncTimeout(
            AsyncRequestTimeoutException e,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        // ✅ WebSocket / SockJS 요청은 응답 바디 없이 종료
        if (isSpecialRequest(request)) {
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
        }

        // ✅ HTTP API는 반드시 JSON으로 응답
        return ResponseEntity
                .status(HttpStatus.REQUEST_TIMEOUT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "success", false,
                        "errorCode", "ASYNC_TIMEOUT",
                        "message", "요청 시간이 초과되었습니다."
                ));
    }



}
