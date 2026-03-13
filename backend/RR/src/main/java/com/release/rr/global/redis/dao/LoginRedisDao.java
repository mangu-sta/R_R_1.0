package com.release.rr.global.redis.dao;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;

// Redis 데이터 접근을 위한 클래스
@Component
public class LoginRedisDao {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ValueOperations<String, Object> values;

    public LoginRedisDao(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.values = redisTemplate.opsForValue(); // String 타입을 쉽게 처리하는 메서드
    }

    // 기본 데이터 저장
    public void setValues(String key, String data) {
        values.set(key, data);
    }

    // 만료 시간이 있는 데이터 저장
    // 주로 RefreshToken 저장할 때 주로 사용함
    public void setValues(String key, String data, Duration duration) {
        values.set(key, data, duration);
    }

    // 데이터 조회
    // RefreshToken 검증 시 사용됨
    public Object getValues(String key) {
        return values.get(key);
    }

    // 데이터 삭제
    // 로그아웃 시 RefreshToken을 삭제할 때 사용함
    public void deleteValues(String key) {
        redisTemplate.delete(key);
    }
}