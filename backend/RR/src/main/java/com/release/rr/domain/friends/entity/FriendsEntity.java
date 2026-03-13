package com.release.rr.domain.friends.entity;

import com.release.rr.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "friends")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendsEntity {

    @EmbeddedId
    private FriendsId id;

    // -------------------- USER (user_id) --------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId") // FriendsId.userId 를 매핑
    @JoinColumn(name = "user_id",
            foreignKey = @ForeignKey(name = "fk_friends_user"))
    private UserEntity user;

    // -------------------- FRIEND (friend_id) --------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("friendId") // FriendsId.friendId 를 매핑
    @JoinColumn(name = "friend_id",
            foreignKey = @ForeignKey(name = "fk_friends_friend"))
    private UserEntity friend;

    // -------------------- 생성 시간 --------------------
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // -------------------- 차단 여부 --------------------
    @Column(name = "is_blocked")
    private Boolean isBlocked = false;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;
}
