package com.release.rr.domain.lobby.service;

import com.release.rr.domain.lobby.dto.LobbyUserDto;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LobbyService {

    // key: userId, value: 유저 정보
    private final Map<Long, LobbyUserDto> users = new ConcurrentHashMap<>();

    public void addUser(Long userId, String nickname, Integer level) {
        LobbyUserDto user = LobbyUserDto.builder()
                .userId(userId)
                .nickname(nickname)
                .level(level)
                .build();
        users.put(userId, user);
    }

    public void removeUser(Long userId) {
        users.remove(userId);
    }

    public Collection<LobbyUserDto> getAllUsers() {
        return users.values();
    }

    public boolean isUserInLobby(Long userId) {
        return users.containsKey(userId);
    }
}
