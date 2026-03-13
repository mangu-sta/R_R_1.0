package com.release.rr.global.redis.dao;

import com.release.rr.domain.map.entity.MapEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RedisGameRoomDao {

    private final StringRedisTemplate redisTemplate;

    private static final String PREFIX = "game:room:";
    private static final String ACTIVE_ROOMS_KEY = "RR:ACTIVE_ROOMS";

    public void createRoom(MapEntity map, List<Long> userIds) {
        String nanoId = map.getNanoId();
        String key = PREFIX + nanoId;

        redisTemplate.opsForHash().put(key, "mapId", map.getMapId().toString());
        redisTemplate.opsForHash().put(key, "nanoId", nanoId);
        redisTemplate.opsForHash().put(
                key,
                "players",
                userIds.stream().map(String::valueOf).collect(Collectors.joining(","))
        );
        redisTemplate.opsForHash().put(key, "status", "IN_GAME");
        
        // ✅ 활성 룸 세트에 추가
        redisTemplate.opsForSet().add(ACTIVE_ROOMS_KEY, nanoId);
    }


    public boolean exists(String nanoId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + nanoId));
    }

    public boolean isPlayerInRoom(String nanoId, Long userId) {
        String key = PREFIX + nanoId;
        Object players = redisTemplate.opsForHash().get(key, "players");
        if (players == null) return false;

        String[] ids = players.toString().split(",");
        for (String id : ids) {
            if (Long.parseLong(id) == userId) {
                return true;
            }
        }
        return false;
    }

    // ✅ userId가 해당 게임방에 속해 있는지 확인
    public boolean isUserInRoom(String nanoId, Long userId) {
        return isPlayerInRoom(nanoId, userId);
    }


    // ✅ 방 안의 플레이어 목록 가져오기
    public List<Long> getPlayerIds(String nanoId) {
        String key = PREFIX + nanoId;
        Object players = redisTemplate.opsForHash().get(key, "players");
        if (players == null || players.toString().isBlank()) return List.of();

        return Arrays.stream(players.toString().split(","))
                .filter(s -> !s.isBlank())
                .map(Long::parseLong)
                .toList();
    }

    // userId -> characterId 저장
    public void putCharacter(String nanoId, Long userId, Long characterId) {
        String key = PREFIX + nanoId + ":characters";
        redisTemplate.opsForHash().put(key, userId.toString(), characterId.toString());
    }

    // userId -> characterId 조회
    public Long getCharacterId(String nanoId, Long userId) {
        String key = PREFIX + nanoId + ":characters";
        Object value = redisTemplate.opsForHash().get(key, userId.toString());
        if (value == null) return null;
        return Long.parseLong(value.toString());
    }

    public List<String> findActiveNanoIds() {
        Set<String> nanoIds = redisTemplate.opsForSet().members(ACTIVE_ROOMS_KEY);
        if (nanoIds == null || nanoIds.isEmpty()) return List.of();
        return new ArrayList<>(nanoIds);
    }

    //방 상태 조회
    public String getStatus(String nanoId) {
        String key = PREFIX + nanoId;
        Object status = redisTemplate.opsForHash().get(key, "status");
        return status == null ? null : status.toString();
    }




    public void addPlayer(String nanoId, Long userId) {
        String key = PREFIX + nanoId;

        Object playersObj = redisTemplate.opsForHash().get(key, "players");
        Set<String> players = new LinkedHashSet<>();

        if (playersObj != null && !playersObj.toString().isBlank()) {
            players.addAll(Arrays.asList(playersObj.toString().split(",")));
        }

        players.add(userId.toString());

        redisTemplate.opsForHash().put(
                key,
                "players",
                String.join(",", players)
        );

        // 최초 생성 시 status
        redisTemplate.opsForHash().putIfAbsent(key, "status", "WAITING");
    }

    public void removePlayer(String nanoId, Long userId) {
        String key = PREFIX + nanoId;

        Object playersObj = redisTemplate.opsForHash().get(key, "players");
        if (playersObj == null) return;

        List<String> players = new ArrayList<>(
                Arrays.asList(playersObj.toString().split(","))
        );

        players.removeIf(id -> id.equals(userId.toString()));

        if (players.isEmpty()) {
            redisTemplate.delete(key);
            redisTemplate.opsForSet().remove(ACTIVE_ROOMS_KEY, nanoId); // ✅ 룸 제거 시 세트에서도 제거
        } else {
            redisTemplate.opsForHash().put(
                    key,
                    "players",
                    String.join(",", players)
            );
        }
    }


    public void clearRoom(String nanoId) {

        String roomKey = PREFIX + nanoId;
        String charMapKey = PREFIX + nanoId + ":characters";

        redisTemplate.delete(roomKey);
        redisTemplate.delete(charMapKey);
        redisTemplate.opsForSet().remove(ACTIVE_ROOMS_KEY, nanoId); // ✅ 룸 명시적 제거 시 세트에서도 제거
 
        System.out.println("🧹 CLEAR ROOM nanoId=" + nanoId);
    }


}
