package com.release.rr.domain.map.service;

import com.release.rr.domain.map.entity.MapEntity;
import com.release.rr.domain.map.repository.MapRepository;
import com.release.rr.global.redis.dao.GameProgressRedisDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MapStageProvider {

    private final GameProgressRedisDao progressRedis;

    /**
     * nanoId → 현재 맵 stage 조회
     */
    public int getStage(String nanoId) {
        return progressRedis.getStage(nanoId);
    }

    /**
     * 편의 메서드
     */
    public boolean isStage1(String nanoId) {
        return getStage(nanoId) == 1;
    }

    /**
     * nanoId → 누적 처치 수 조회 (Stage1 진행도)
     */
    public int getKillCount(String nanoId) {
        return progressRedis.getKillCount(nanoId);
    }

}
