package com.release.rr.global.redis.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class KillCountDedupRedisDao {

    private final StringRedisTemplate redisTemplate;

    private String key(String nanoId, Long monsterId) {
        return "RR:KILLCOUNTED:" + nanoId + ":" + monsterId;
    }

    public boolean markOnce(String nanoId, Long monsterId) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key(nanoId, monsterId), "1", Duration.ofHours(6));
        return Boolean.TRUE.equals(success);
    }
}
