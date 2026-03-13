package com.release.rr.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    // -------------------- 닉네임 --------------------
    @Column(nullable = false, length = 20)
    private String nickname;

    // -------------------- 비밀번호 --------------------
    @Column(nullable = false, length = 255)
    private String password;

    // -------------------- 가입 시간 --------------------
    @CreationTimestamp
    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    // -------------------- IP --------------------
    @Column(nullable = false, length = 45)
    private String ip;

    // -------------------- 권한 레벨 --------------------
    // 0 = 일반회원, 9 = 관리자
    private Integer level = 0;

    // -------------------- 키 설정(JSON) --------------------
    @Lob
    @Column(name = "key_config", columnDefinition = "TEXT")
    private String keyConfigJson;

}
