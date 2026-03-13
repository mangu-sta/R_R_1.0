package com.release.rr.domain.notifications.controller;

import com.release.rr.domain.notifications.dto.NotificationDto;
import com.release.rr.domain.notifications.service.NotificationService;
import com.release.rr.global.exception.CustomException;
import com.release.rr.global.exception.ErrorCode;
import com.release.rr.global.security.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping(value = "/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtProvider jwtProvider;

    /**
     * 쿠키에서 userId 가져오기 (토큰이 없으면 null 반환)
     */
    private Long getCurrentUserId(HttpServletRequest req) {
        String token = jwtProvider.extractAccessTokenFromCookie(req);
        if (token == null || token.isEmpty()) {
            return null;
        }
        return jwtProvider.getUserId(token);
    }

    /**
     * SSE 구독 (로그인한 유저만 가능)
     */
    @GetMapping("/subscribe")
    public SseEmitter subscribe(HttpServletRequest req) {

        Long userId = getCurrentUserId(req);

        if (userId == null) {
            // 로그인 안 되어 있으면 SSE 연결하지 않음
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        return notificationService.subscribe(userId);
    }


    /**
     * 🎯 Redis OFF 모드에서는 알림 목록 조회 기능도 OFF
     * 필요하다면 이후 다시 구현
     */
    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.badRequest().body("알림 저장 기능을 사용하지 않는 모드입니다.");
    }

    /**
     * 🎯 Redis OFF 모드에서는 clear 기능도 OFF
     */
    @DeleteMapping
    public ResponseEntity<?> clear() {
        return ResponseEntity.badRequest().body("알림 저장 기능을 사용하지 않는 모드입니다.");
    }
}
