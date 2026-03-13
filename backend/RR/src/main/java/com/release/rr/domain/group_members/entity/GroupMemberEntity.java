package com.release.rr.domain.group_members.entity;

import com.release.rr.domain.map.entity.MapEntity;
import com.release.rr.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMemberEntity {

    @EmbeddedId
    private GroupMemberId id;

    // ----------- MAP FK (map_id) -----------
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("mapId") // 복합키의 mapId 매핑
    @JoinColumn(name = "map_id",
            foreignKey = @ForeignKey(name = "fk_gm_map"))
    private MapEntity map;

    // ----------- USER FK (user_id) -----------
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId") // 복합키의 userId 매핑
    @JoinColumn(name = "user_id",
            foreignKey = @ForeignKey(name = "fk_gm_user"))
    private UserEntity user;

    // ----------- 참여 시간 (joined_at) -----------
    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;
}
