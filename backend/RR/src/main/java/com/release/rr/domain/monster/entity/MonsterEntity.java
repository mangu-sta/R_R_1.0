package com.release.rr.domain.monster.entity;

import com.release.rr.domain.map.entity.MapEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "monsters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonsterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ----------------- MAP FK (map_id) -----------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "map_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_monster_map"))
    private MapEntity map;

    // ----------------- 몬스터 타입 ENUM -----------------
    public enum MonsterType {
        NORMAL, NAMED, BOSS
    }

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MonsterType type = MonsterType.NORMAL;

    // ----------------- 몬스터 이름 ENUM -----------------
    public enum MonsterName {
        SLOW, RUNNER, RANGER, HEALTH, FAST_RANGER, INVISIBLE, BOMBER
    }

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MonsterName name = MonsterName.SLOW;

    // ----------------- 좌표 -----------------
    @Column(name = "pos_x", nullable = false)
    private float posX;

    @Column(name = "pos_y", nullable = false)
    private float posY;

    // ----------------- 스탯 -----------------
    @Builder.Default
    private float hp = 100f;

    @Builder.Default
    @Column(name = "max_hp")
    private float maxHp = 100f;

    @Builder.Default
    private float damage = 5f;

    @Builder.Default
    private float attackSpeed = 10f;

    @Builder.Default
    private float speed = 10f;

    @Builder.Default
    @Column(name = "`range`")
    private float range = 10f;

    @Builder.Default
    private int exp = 0;
}
