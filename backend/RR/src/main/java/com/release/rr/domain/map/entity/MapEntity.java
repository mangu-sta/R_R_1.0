package com.release.rr.domain.map.entity;

import com.release.rr.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "maps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MapEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "map_id")
    private Long mapId;

    // ---------- 공통적으로 id 를 반환하도록 getId() 제공 ----------
    public Long getId() {
        return this.mapId;
    }

    // ---------- Nano ID (추가) ----------
    @Column(name = "nano_id", length = 16, nullable = false, unique = true)
    private String nanoId;

    // ---------- MAP OWNER ----------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "map_owner", nullable = false, foreignKey = @ForeignKey(name = "fk_maps_owner_users"))
    private UserEntity owner;

    // ---------- 맵 이름 ----------
    @Column(name = "map_name", nullable = false, length = 64)
    private String mapName;

    // ---------- 생성일 ----------
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ---------- 스테이지 ----------
    @Builder.Default
    @Column(name = "stage")
    private Integer stage = 0;

    // ---------- 누적 처치수 (Stage1 진행도) ----------
    @Builder.Default
    @Column(name = "kill_count", nullable = false)
    private Integer killCount = 0;
}
