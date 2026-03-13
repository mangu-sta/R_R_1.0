package com.release.rr.global.redis.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameProgressRedisDao {

    private final StringRedisTemplate redisTemplate;

    private String key(String nanoId) {
        return "RR:GAME:" + nanoId + ":STATE";
    }

    /** 최초 진입 시 */
    public void initIfAbsent(String nanoId) {
        String k = key(nanoId);
        Boolean a = redisTemplate.opsForHash().putIfAbsent(k, "stage", "0");
        Boolean b = redisTemplate.opsForHash().putIfAbsent(k, "killCount", "0");
        System.out.println("[GameProgZressRedisDao] initIfAbsent key=" + k + " stagePut=" + a + " killPut=" + b);
    }

    public void forceInit(String nanoId) {
        String k = key(nanoId);
        redisTemplate.opsForHash().put(k, "stage", "0");
        redisTemplate.opsForHash().put(k, "killCount", "0");
        redisTemplate.opsForHash().delete(k, "startTime"); // Reset start time
        System.out.println("[GameProgressRedisDao] forceInit key=" + k + " entries=" + redisTemplate.opsForHash().entries(k));
    }


    public int getStage(String nanoId) {
        Object v = redisTemplate.opsForHash().get(key(nanoId), "stage");
        return v == null ? 0 : Integer.parseInt(v.toString());
    }

    public void setStage(String nanoId, int stage) {
        redisTemplate.opsForHash()
                .put(key(nanoId), "stage", String.valueOf(stage));
    }


    public int getKillCount(String nanoId) {
        Object v = redisTemplate.opsForHash().get(key(nanoId), "killCount");
        return v == null ? 0 : Integer.parseInt(v.toString());
    }

    public void setKillCount(String nanoId, int count) {
        redisTemplate.opsForHash()
                .put(key(nanoId), "killCount", String.valueOf(count));
    }

    public void increaseKill(String nanoId) {
        redisTemplate.opsForHash()
                .increment(key(nanoId), "killCount", 1);
    }

    public void clear(String nanoId) {
        System.out.println("❌ CLEAR GAME PROGRESS nanoId=" + nanoId);

        redisTemplate.delete(key(nanoId));
    }





    public void setStage1StartTime(String nanoId, long timestamp) {
        String k = key(nanoId);
        redisTemplate.opsForHash().put(k, "startTime", String.valueOf(timestamp));
    }

    public Long getStage1StartTime(String nanoId) {
        Object v = redisTemplate.opsForHash().get(key(nanoId), "startTime");
        return v == null ? null : Long.parseLong(v.toString());
    }

}

