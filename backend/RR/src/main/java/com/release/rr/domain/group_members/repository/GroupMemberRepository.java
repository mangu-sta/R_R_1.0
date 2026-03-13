package com.release.rr.domain.group_members.repository;


import com.release.rr.domain.group_members.entity.GroupMemberEntity;
import com.release.rr.domain.map.entity.MapEntity;
import com.release.rr.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupMemberRepository extends JpaRepository<GroupMemberEntity, Long> {
    List<GroupMemberEntity> findByUser(UserEntity user);
    List<GroupMemberEntity> findByMap(MapEntity map);
}
