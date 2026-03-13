package com.release.rr.domain.map.service;

import com.release.rr.domain.characters.entity.CharacterEntity;
import com.release.rr.domain.characters.repository.CharacterRepository;
import com.release.rr.domain.map.dto.GameCharacterDto;
import com.release.rr.domain.map.dto.GameInitStateDto;
import com.release.rr.domain.map.dto.GameMonsterDto;
import com.release.rr.global.redis.dao.MonsterStateRedisDao;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameInitService {

    private final CharacterRepository characterRepository;
    private final MonsterStateRedisDao monsterStateRedisDao;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public void sendInitialStateToUser(String nanoId, Long userId) {

        // 1️⃣ 플레이어 목록
        List<GameCharacterDto> players =
                characterRepository.findByMap_NanoId(nanoId).stream()
                        .map(GameCharacterDto::from)
                        .toList();

        // 2️⃣ 몬스터 목록 (Redis 기준)
        List<GameMonsterDto> monsters =
                monsterStateRedisDao.findAllByMap(nanoId).stream()
                        .map(GameMonsterDto::from)
                        .toList();

        // 3️⃣ INIT 패킷 전송
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/game/init",
                new GameInitStateDto(players, monsters)
        );
    }
}

