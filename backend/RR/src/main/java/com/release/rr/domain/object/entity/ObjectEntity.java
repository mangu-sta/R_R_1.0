package com.release.rr.domain.object.entity;

import com.release.rr.domain.map.entity.MapEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "objects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ----------------- MAP FK (map_id) -----------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "map_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_object_map"))
    private MapEntity map;

    // ----------------- ENUM: object_name -----------------
    public enum ObjectName {
        BOX, BOMB
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "object_name", nullable = false)
    private ObjectName objectName = ObjectName.BOX;

    // ----------------- 좌표 -----------------
    @Column(name = "pos_x", nullable = false)
    private Integer posX;

    @Column(name = "pos_y", nullable = false)
    private Integer posY;

    // ----------------- 크기 -----------------
    private float width = 10f;
    private float height = 10f;

    // ----------------- 파괴 여부 -----------------
    @Column(name = "is_break")
    private Boolean isBreak = false;
}
