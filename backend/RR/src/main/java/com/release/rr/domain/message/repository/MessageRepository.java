package com.release.rr.domain.message.repository;

import com.release.rr.domain.message.entity.MessageEntity;
import com.release.rr.domain.map.entity.MapEntity;
import com.release.rr.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    List<MessageEntity> findByMap(MapEntity map);
    List<MessageEntity> findByUser(UserEntity user);
}
