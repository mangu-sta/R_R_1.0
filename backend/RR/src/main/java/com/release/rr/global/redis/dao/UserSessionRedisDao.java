// com.release.rr.global.redis.dao.UserSessionRedisDao

package com.release.rr.global.redis.dao;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class UserSessionRedisDao {

    private final StringRedisTemplate redis;

    // Access / Refresh TTL (Redis 기준)
    private static final Duration ACCESS_TTL  = Duration.ofMinutes(30);    // 30분
    private static final Duration REFRESH_TTL = Duration.ofDays(14);       // 14일

    private String accessKey(Long userId) {
        return "RR:ACCESS:" + userId;
    }

    private String refreshKey(Long userId) {
        return "RR:REFRESH:" + userId;
    }

    // -------- Access Token --------
    public void saveAccessToken(Long userId, String token) {
        redis.opsForValue().set(accessKey(userId), token, ACCESS_TTL);
    }

    public String getAccessToken(Long userId) {
        return redis.opsForValue().get(accessKey(userId));
    }

    public void deleteAccessToken(Long userId) {
        redis.delete(accessKey(userId));
    }

    // -------- Refresh Token --------
    public void saveRefreshToken(Long userId, String token) {
        redis.opsForValue().set(refreshKey(userId), token, REFRESH_TTL);
    }

    public String getRefreshToken(Long userId) {
        return redis.opsForValue().get(refreshKey(userId));
    }

    public void deleteRefreshToken(Long userId) {
        redis.delete(refreshKey(userId));
    }

    // 전체 세션 삭제
    public void deleteAllTokens(Long userId) {
        deleteAccessToken(userId);
        deleteRefreshToken(userId);
    }
}
