/*
package com.release.rr.global.redis.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.release.rr.domain.notifications.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationRedisDao {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    private String key(Long userId) {
        // 유저별 알림 리스트 키
        return "RR:NOTI:" + userId;
    }

    // 알림 1개 저장 (리스트 push)
    public void pushNotification(Long userId, NotificationDto dto) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            redis.opsForList().leftPush(key(userId), json);
            // 알림 key 자체를 며칠까지만 유지 (예: 7일)
            redis.expire(key(userId), Duration.ofDays(7));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("알림 직렬화 실패", e);
        }
    }

    // 전체 알림 가져오기
    public List<NotificationDto> getAll(Long userId) {
        List<String> list = redis.opsForList().range(key(userId), 0, -1);
        List<NotificationDto> result = new ArrayList<>();
        if (list == null) return result;

        for (String json : list) {
            try {
                NotificationDto dto = objectMapper.readValue(json, NotificationDto.class);
                result.add(dto);
            } catch (JsonProcessingException e) {
                // 개별 실패는 무시하고 계속 진행
            }
        }
        return result;
    }

    // 알림 전부 삭제
    public void clear(Long userId) {
        redis.delete(key(userId));
    }
}
*/
