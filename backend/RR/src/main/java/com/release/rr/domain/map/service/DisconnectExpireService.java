package com.release.rr.domain.map.service;

import com.release.rr.domain.characters.entity.CharacterEntity;
import com.release.rr.domain.characters.repository.CharacterRepository;
import com.release.rr.global.redis.dao.CharacterStateRedisDao;
import com.release.rr.global.redis.dao.UserGameStateRedisDao;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DisconnectExpireService {

    private final UserGameStateRedisDao userGameStateRedisDao;
    private final CharacterRepository characterRepository;
    private final CharacterStateRedisDao characterStateRedisDao;

    // 5초마다 검사
    @Scheduled(fixedDelay = 5000)
    public void expireDisconnectedUsers() {

        // Redis SCAN으로 disconnect 키 조회 (간단 버전은 생략)
        // 실제로는 Set/Prefix 관리 추천

        // 👉 1차 버전에서는 “TTL 만료 후 join 시 처리”도 OK
    }
}
