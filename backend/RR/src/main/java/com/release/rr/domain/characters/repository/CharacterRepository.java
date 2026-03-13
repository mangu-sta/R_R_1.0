package com.release.rr.domain.characters.repository;

import com.release.rr.domain.characters.entity.CharacterEntity;
import com.release.rr.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CharacterRepository extends JpaRepository<CharacterEntity, Long> {
    Optional<CharacterEntity> findByUser(UserEntity user);
    void deleteAllByUser(UserEntity user);
    boolean existsByUser(UserEntity user);
    Optional<CharacterEntity> findByUser_UserId(Long userId);
    // map_id 기준으로 캐릭터 조회
    List<CharacterEntity> findByMap_MapId(Long mapId);
    // (선택) nanoId 기준 조회 버전 (Fetch Join 추가)
    @Query("select c from CharacterEntity c join fetch c.user where c.map.nanoId = :nanoId")
    List<CharacterEntity> findByMap_NanoId(@Param("nanoId") String nanoId);


}
