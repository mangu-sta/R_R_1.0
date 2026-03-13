package com.release.rr.domain.friends.repository;

import com.release.rr.domain.friend_requests.entity.FriendRequestEntity;
import com.release.rr.domain.friends.entity.FriendsEntity;
import com.release.rr.domain.friends.entity.FriendsId;
import com.release.rr.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendsRepository extends JpaRepository<FriendsEntity, FriendsId> {

    // 친구 목록 조회
    List<FriendsEntity> findByUser_UserId(Long userId);

    // 친구 관계 여부 체크
    boolean existsByUser_UserIdAndFriend_UserId(Long userId, Long friendId);

    // 삭제용
    void deleteByUser_UserIdAndFriend_UserId(Long userId, Long friendId);

    Optional<FriendsEntity> findByUser_UserIdAndFriend_UserId(Long userId, Long friendId);

}

