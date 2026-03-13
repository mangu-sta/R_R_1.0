package com.release.rr.domain.weapon.repository;

import com.release.rr.domain.weapon.entity.WeaponEntity;
import com.release.rr.domain.object.entity.ObjectEntity;
import com.release.rr.domain.characters.entity.CharacterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeaponRepository extends JpaRepository<WeaponEntity, Long> {
    List<WeaponEntity> findByObject(ObjectEntity object);
    List<WeaponEntity> findByCharacter(CharacterEntity character);
}
