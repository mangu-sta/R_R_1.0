package com.release.rr.domain.message.entity;

import com.release.rr.domain.map.entity.MapEntity;
import com.release.rr.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    // --------------- MAP FK (group_id) ---------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_msg_map"))
    private MapEntity map;

    // --------------- USER FK (user_id) ---------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_msg_user"))
    private UserEntity user;

    // --------------- 내용 ---------------
    @Column(columnDefinition = "TEXT")
    private String content;

    // --------------- 전송 시각 ---------------
    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;
}
