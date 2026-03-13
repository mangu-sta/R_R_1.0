package com.release.rr.domain.characters.entity;
import com.release.rr.domain.characters.mapper.CharacterStatMapper;
import com.release.rr.domain.characters.stat.CharacterStatSnapshot;
import com.release.rr.domain.user.entity.UserEntity;
import com.release.rr.domain.map.entity.MapEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "characters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ----------- USER FK -----------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_char_user"))
    private UserEntity user;

    // ----------- JOB ENUM -----------
    public enum Job {
        FIREFIGHTER, SOLDIER, DOCTOR, REPORTER
    }

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Job job = Job.FIREFIGHTER;

    // ----------- 기본 스탯들 -----------
    @Builder.Default
    private Integer level = 0;
    
    @Builder.Default
    private Integer exp = 0;

    @Builder.Default
    @Transient
    private Integer pendingStatPoints = 0;

    // ----------- MAP FK -----------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "map_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_char_map"))
    private MapEntity map;

    // ----------- 좌표 / 체력 -----------
    @Builder.Default
    @Column(name = "pos_x")
    private float posX = 0.0f;

    @Builder.Default
    @Column(name = "pos_y")
    private float posY = 0.0f;

    private float hp;

    @Column(name = "max_hp")
    private float maxHp;

    // ----------- JSON 스탯 -----------
    @Column(columnDefinition = "LONGTEXT")
    private String status;

    // ----------------- 시선 각도 -----------------
    @Column(name = "angle",nullable = false)
    private float angle;


    // ----------- 생성일 -----------
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ----------- 엔딩 여부 -----------
    @Builder.Default
    @Column(name = "is_end")
    private Boolean isEnd = false;


    @Transient
    public CharacterStatSnapshot getStatSnapshot() {
        try {
            return CharacterStatMapper.fromStatusJson(this.status);
        } catch (Exception e) {
            // Log the exception if necessary
            return new CharacterStatSnapshot(0, 0, 0, 0, 0);
        }
    }

}
