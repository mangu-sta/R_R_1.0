package com.release.rr.domain.friends.service;

import com.release.rr.domain.friends.dto.FriendDto;
import com.release.rr.domain.friends.entity.FriendsEntity;
import com.release.rr.domain.friends.repository.FriendsRepository;
import com.release.rr.domain.user.entity.UserEntity;
import com.release.rr.domain.user.repository.UserRepository;
import com.release.rr.global.exception.CustomException;
import com.release.rr.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendsRepository friendsRepo;
    private final UserRepository userRepository;

    // -------------------- 친구 목록 --------------------
    public List<FriendDto> getFriends(Long userId) {
        return friendsRepo.findByUser_UserId(userId)
                .stream()
                .filter(f -> !Boolean.TRUE.equals(f.getIsBlocked()))
                .map(f -> FriendDto.builder()
                        .nickname(f.getFriend().getNickname())
                        .build())
                .toList();
    }

    // -------------------- 친구 삭제 --------------------
    @Transactional
    public void deleteFriend(Long userId, String friendNickname) {

        UserEntity friend = userRepository.findByNickname(friendNickname)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Long friendId = friend.getUserId();

        if (!friendsRepo.existsByUser_UserIdAndFriend_UserId(userId, friendId)) {
            throw new CustomException(ErrorCode.FRIEND_NOT_FOUND);
        }

        friendsRepo.deleteByUser_UserIdAndFriend_UserId(userId, friendId);
        friendsRepo.deleteByUser_UserIdAndFriend_UserId(friendId, userId);
    }

    // -------------------- 친구 차단 --------------------
    public void blockFriend(Long userId, String targetNickname) {

        UserEntity target = userRepository.findByNickname(targetNickname)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        FriendsEntity relation = friendsRepo.findByUser_UserIdAndFriend_UserId(userId, target.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.FRIEND_NOT_FOUND));

        if (Boolean.TRUE.equals(relation.getIsBlocked())) {
            throw new CustomException(ErrorCode.FRIEND_ALREADY_BLOCKED);
        }

        relation.setIsBlocked(true);
        relation.setBlockedAt(LocalDateTime.now());

        friendsRepo.save(relation);
    }

    // -------------------- 차단 해제 --------------------
    public void unblockFriend(Long userId, String targetNickname) {

        UserEntity target = userRepository.findByNickname(targetNickname)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        FriendsEntity relation = friendsRepo.findByUser_UserIdAndFriend_UserId(userId, target.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.FRIEND_NOT_FOUND));

        if (!Boolean.TRUE.equals(relation.getIsBlocked())) {
            throw new CustomException(ErrorCode.FRIEND_NOT_BLOCKED);
        }

        relation.setIsBlocked(false);
        relation.setBlockedAt(null);

        friendsRepo.save(relation);
    }
}
