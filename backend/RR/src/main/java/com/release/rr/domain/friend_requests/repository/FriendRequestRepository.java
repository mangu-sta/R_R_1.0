package com.release.rr.domain.friend_requests.repository;

import com.release.rr.domain.friend_requests.entity.FriendRequestEntity;
import com.release.rr.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequestEntity, Long> {

    // 요청 중복 확인용
    boolean existsBySender_UserIdAndReceiver_UserId(Long senderId, Long receiverId);

    // 받은 요청 목록
    List<FriendRequestEntity> findByReceiver_UserId(Long receiverId);

    // 요청 검색
    Optional<FriendRequestEntity> findBySender_UserIdAndReceiver_UserId(Long senderId, Long receiverId);
    List<FriendRequestEntity> findBySender_UserId(Long senderId);


}
