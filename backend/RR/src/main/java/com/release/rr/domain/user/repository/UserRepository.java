package com.release.rr.domain.user.repository;

import com.release.rr.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByNickname(String nickname);

    boolean existsByNickname(String nickname);
}
