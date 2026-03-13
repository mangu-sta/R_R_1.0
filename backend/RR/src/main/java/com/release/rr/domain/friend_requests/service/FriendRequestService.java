package com.release.rr.domain.friend_requests.service;

import com.release.rr.domain.friend_requests.dto.FriendRequestDto;
import com.release.rr.domain.friend_requests.dto.FriendRequestSentDto;
import com.release.rr.domain.friend_requests.entity.FriendRequestEntity;
import com.release.rr.domain.friend_requests.repository.FriendRequestRepository;
import com.release.rr.domain.friends.entity.FriendsEntity;
import com.release.rr.domain.friends.entity.FriendsId;
import com.release.rr.domain.friends.repository.FriendsRepository;
import com.release.rr.domain.notifications.dto.NotificationDto;
import com.release.rr.domain.notifications.dto.NotificationType;
import com.release.rr.domain.notifications.service.NotificationService;
import com.release.rr.domain.user.entity.UserEntity;
import com.release.rr.domain.user.repository.UserRepository;
import com.release.rr.global.exception.CustomException;
import com.release.rr.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendRequestService {

    private final UserRepository userRepository;
    private final FriendRequestRepository requestRepo;
    private final FriendsRepository friendsRepo;
    private final NotificationService notificationService;


    // -------------------- 친구 신청 --------------------
    public void sendFriendRequest(Long senderId, String receiverNickname) {

        UserEntity receiver = userRepository.findByNickname(receiverNickname)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (senderId.equals(receiver.getUserId())) {
            throw new CustomException(ErrorCode.CANNOT_ADD_SELF);
        }

        if (friendsRepo.existsByUser_UserIdAndFriend_UserId(senderId, receiver.getUserId())) {
            throw new CustomException(ErrorCode.FRIEND_ALREADY);
        }

        if (requestRepo.existsBySender_UserIdAndReceiver_UserId(senderId, receiver.getUserId())) {
            throw new CustomException(ErrorCode.FRIEND_REQUEST_ALREADY_SENT);
        }

        UserEntity sender = userRepository.findById(senderId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        requestRepo.save(FriendRequestEntity.builder()
                .sender(sender)
                .receiver(receiver)
                .build());

        // 2) ⭐ 알림 전송
        NotificationDto noti = NotificationDto.builder()
                .type(NotificationType.FRIEND_REQUEST)
                .senderId(sender.getUserId())
                .senderNickname(sender.getNickname())
                .message(sender.getNickname() + " 님이 친구 요청을 보냈습니다.")
                .createdAt(LocalDateTime.now())
                .build();

        notificationService.notify(receiver.getUserId(), noti);


    }

    // -------------------- 받은 요청 목록 --------------------
    public List<FriendRequestDto> getReceivedRequests(Long userId) {

        return requestRepo.findByReceiver_UserId(userId)
                .stream()
                .map(req -> FriendRequestDto.builder()
                        .requestId(req.getRequestId())
                        .senderId(req.getSender().getUserId())
                        .senderNickname(req.getSender().getNickname())
                        .sentAt(req.getCreatedAt().toString())
                        .build())
                .toList();
    }

    // -------------------- 보낸 요청 목록 --------------------
    public List<FriendRequestSentDto> getSentRequests(Long userId) {

        return requestRepo.findBySender_UserId(userId)
                .stream()
                .map(req -> FriendRequestSentDto.builder()
                        .requestId(req.getRequestId())
                        .receiverId(req.getReceiver().getUserId())
                        .receiverNickname(req.getReceiver().getNickname())
                        .sentAt(req.getCreatedAt().toString())
                        .build())
                .toList();
    }

    // -------------------- 요청 수락 --------------------
    public void acceptRequest(Long senderId, Long receiverId) {

        FriendRequestEntity req = requestRepo.findBySender_UserIdAndReceiver_UserId(senderId, receiverId)
                .orElseThrow(() -> new CustomException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        requestRepo.delete(req);

        UserEntity sender = req.getSender();
        UserEntity receiver = req.getReceiver();

        friendsRepo.save(new FriendsEntity(new FriendsId(sender.getUserId(), receiver.getUserId()), sender, receiver, null, false, null));
        friendsRepo.save(new FriendsEntity(new FriendsId(receiver.getUserId(), sender.getUserId()), receiver, sender, null, false, null));

        // ⭐ 알림 전송
        NotificationDto noti = NotificationDto.builder()
                .type(NotificationType.FRIEND_ACCEPT)
                .senderId(receiver.getUserId())
                .senderNickname(receiver.getNickname())
                .message(receiver.getNickname() + " 님이 친구 요청을 수락했습니다.")
                .createdAt(LocalDateTime.now())
                .build();

        notificationService.notify(sender.getUserId(), noti);

    }

    // -------------------- 요청 거절 --------------------
    public void rejectRequest(Long senderId, Long receiverId) {

        FriendRequestEntity req = requestRepo.findBySender_UserIdAndReceiver_UserId(senderId, receiverId)
                .orElseThrow(() -> new CustomException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        requestRepo.delete(req);

        // ⭐ 알림 전송
        UserEntity receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        NotificationDto noti = NotificationDto.builder()
                .type(NotificationType.FRIEND_REJECT)
                .senderId(receiver.getUserId())
                .senderNickname(receiver.getNickname())
                .message(receiver.getNickname() + " 님이 친구 요청을 거절했습니다.")
                .createdAt(LocalDateTime.now())
                .build();

        notificationService.notify(senderId, noti);
    }

    // -------------------- 보낸 친구 요청 취소 --------------------
    public void cancelSentRequest(Long senderId, Long receiverId) {

        // senderId → 보낸 사람
        // receiverId → 취소하려는 친구 요청의 대상자

        FriendRequestEntity req = requestRepo
                .findBySender_UserIdAndReceiver_UserId(senderId, receiverId)
                .orElseThrow(() -> new CustomException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        // 요청 삭제 (취소)
        requestRepo.delete(req);

        // ⭐ 알림 전송
        UserEntity sender = userRepository.findById(senderId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        NotificationDto noti = NotificationDto.builder()
                .type(NotificationType.FRIEND_CANCEL)
                .senderId(senderId)
                .senderNickname(sender.getNickname())
                .message(sender.getNickname() + " 님이 보낸 친구 요청이 취소되었습니다.")
                .createdAt(LocalDateTime.now())
                .build();

        notificationService.notify(receiverId, noti);

    }

}
