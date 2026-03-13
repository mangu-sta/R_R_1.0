package com.release.rr.domain.map.repository;

import com.release.rr.domain.map.entity.MapEntity;
import com.release.rr.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MapRepository extends JpaRepository<MapEntity, Long> {
    MapEntity findByMapName(String mapName);
    Optional<MapEntity> findByOwner(UserEntity owner);
    Optional<MapEntity> findByOwnerUserId(Long userId);
    Optional<MapEntity> findByNanoId(String nanoId);
    @Query("select m.stage from MapEntity m where m.nanoId = :nanoId")
    Integer findStageByNanoId(@Param("nanoId") String nanoId);
    @Modifying
    @Query("""
        update MapEntity m
           set m.killCount = m.killCount + 1
         where m.nanoId = :nanoId
    """)
    int increaseKillCount(@Param("nanoId") String nanoId);

    @Modifying
    @Query("update MapEntity m set m.stage = :stage where m.nanoId = :nanoId")
    int updateStage(@Param("nanoId") String nanoId, @Param("stage") int stage);

    @Modifying
    @Query("""
    update MapEntity m
    set m.stage = 1
    where m.nanoId = :nanoId
      and m.stage = 0
    """)
    int advanceStage0To1(@Param("nanoId") String nanoId);


}
