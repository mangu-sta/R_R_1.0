package com.release.rr.global.redis.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class UserGameStateRedisDao {

    private final StringRedisTemplate redisTemplate;

    private static final String PREFIX = "user:state:";
    private static final String DISCONNECT_PREFIX = "user:disconnect:";

    public void enterGame(Long userId, String roomId) {
        String key = PREFIX + userId;
        redisTemplate.opsForHash().put(key, "state", "IN_GAME");
        redisTemplate.opsForHash().put(key, "roomId", roomId);
    }

    public void leaveGame(Long userId) {
        redisTemplate.delete(PREFIX + userId);
    }

    public String getRoomId(Long userId) {
        Object value = redisTemplate.opsForHash()
                .get(PREFIX + userId, "roomId");
        return value != null ? value.toString() : null;
    }

    public void markDisconnected(Long userId, long seconds) {
        String key = DISCONNECT_PREFIX + userId;
        redisTemplate.opsForValue().set(key, "1", seconds, TimeUnit.SECONDS);
    }

    public void clearDisconnectTimer(Long userId) {
        redisTemplate.delete(DISCONNECT_PREFIX + userId);
    }

    public boolean isDisconnectExpired(Long userId) {
        return !Boolean.TRUE.equals(
                redisTemplate.hasKey(DISCONNECT_PREFIX + userId)
        );
    }



}
