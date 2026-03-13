package com.release.rr.domain.monster.repository;

import com.release.rr.domain.monster.entity.MonsterEntity;
import com.release.rr.domain.map.entity.MapEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonsterRepository extends JpaRepository<MonsterEntity, Long> {
    List<MonsterEntity> findByMap_MapId(Long mapId);
    boolean existsByMap_MapId(Long mapId);


}
