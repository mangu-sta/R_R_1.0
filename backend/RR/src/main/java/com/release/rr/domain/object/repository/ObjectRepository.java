package com.release.rr.domain.object.repository;

import com.release.rr.domain.object.entity.ObjectEntity;
import com.release.rr.domain.map.entity.MapEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObjectRepository extends JpaRepository<ObjectEntity, Long> {
    List<ObjectEntity> findByMap(MapEntity map);
}
