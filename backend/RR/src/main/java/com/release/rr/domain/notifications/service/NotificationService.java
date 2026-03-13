package com.release.rr.domain.notifications.service;

import com.release.rr.domain.notifications.dto.NotificationDto;
import com.release.rr.domain.notifications.dto.NotificationType;
import com.release.rr.domain.notifications.repository.SseEmitterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.Console;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Long DEFAULT_TIMEOUT = 60L * 60 * 1000;

    private final SseEmitterRepository emitterRepository;

    // SSE 구독 처리
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitterRepository.save(userId, emitter);

        emitter.onCompletion(() -> emitterRepository.delete(userId));
        emitter.onTimeout(() -> emitterRepository.delete(userId));

        // 연결 직후 단 1번만 전송
        sendToClient(userId, NotificationDto.builder()
                .type(NotificationType.CONSOLE)
                .senderId(null)
                .senderNickname(null)
                .message("SSE connected")
                .createdAt(LocalDateTime.now())
                .read(false)
                .build());

        return emitter;
    }

    // 외부 서비스에서 호출하는 실시간 알림
    public void notify(Long targetUserId, NotificationDto dto) {

        dto.setCreatedAt(LocalDateTime.now());
        dto.setRead(false);

        // Redis 저장 없이 → 실시간 전송만!
        sendToClient(targetUserId, dto);
    }

    // SSE 전송
    private void sendToClient(Long userId, NotificationDto dto) {
        SseEmitter emitter = emitterRepository.get(userId);
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(dto));
        } catch (IOException e) {
            emitterRepository.delete(userId);
            emitter.completeWithError(e);
        }
    }
}

