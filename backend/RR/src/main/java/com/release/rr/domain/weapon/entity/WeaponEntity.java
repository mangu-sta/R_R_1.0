package com.release.rr.domain.weapon.entity;

import com.release.rr.domain.characters.entity.CharacterEntity;
import com.release.rr.domain.object.entity.ObjectEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "weapons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeaponEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ----------------- OBJECT FK (object_id) -----------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "object_id",
            foreignKey = @ForeignKey(name = "fk_weapon_object"))
    private ObjectEntity object;

    // ----------------- CHARACTER FK (character_id) -----------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id",
            foreignKey = @ForeignKey(name = "fk_weapon_character"))
    private CharacterEntity character;

    // ----------------- 무기 이름 -----------------
    @Column(length = 50)
    private String name;

    // ----------------- ENUM: rarity -----------------
    public enum Rarity {
        COMMON, RARE, EPIC, LEGENDARY
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rarity rarity = Rarity.COMMON;

    // ----------------- 스탯 -----------------
    private float damage = 1f;

    @Column(name = "attack_speed")
    private float attackSpeed = 1f;

    @Column(name = "reload_speed")
    private float reloadSpeed = 1f;

    // ----------------- ENUM: type -----------------
    public enum WeaponType {
        GUN, MELEE
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WeaponType type = WeaponType.GUN;

    // ----------------- 탄약 -----------------
    private Integer ammo = 0;
}
