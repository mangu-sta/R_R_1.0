package com.release.rr.domain.notifications.repository;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class SseEmitterRepository {

    // userId → 그 유저의 SSE 연결 1개 (탭 여러 개면 확장 가능)
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void save(Long userId, SseEmitter emitter) {
        emitters.put(userId, emitter);
    }

    public SseEmitter get(Long userId) {
        return emitters.get(userId);
    }

    public void delete(Long userId) {
        emitters.remove(userId);
    }
}
