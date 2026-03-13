package com.release.rr.domain.friend_requests.controller;

import com.release.rr.domain.friend_requests.dto.FriendRequestCancelDto;
import com.release.rr.domain.friend_requests.service.FriendRequestService;
import com.release.rr.domain.friends.dto.FriendAcceptDto;
import com.release.rr.domain.friends.dto.FriendRejectDto;
import com.release.rr.domain.friends.dto.FriendRequestSendDto;
import com.release.rr.global.security.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/friend-request"
)
@RequiredArgsConstructor
public class FriendRequestController {

    private final FriendRequestService requestService;
    private final JwtProvider jwtProvider;

    // 🍪 쿠키에서 Access Token 추출 → userId 반환
    private Long current(HttpServletRequest req) {
        return jwtProvider.getUserId(jwtProvider.extractAccessTokenFromCookie(req));
    }

    // -------------------- 친구 요청 보내기 --------------------
    // 클라이언트가 receiverNickname 을 보내면, 현재 로그인한 유저가 해당 닉네임에게 친구 요청을 보냄
    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody FriendRequestSendDto dto, HttpServletRequest req) {
        requestService.sendFriendRequest(current(req), dto.getReceiverNickname());
        return ResponseEntity.ok("친구 요청 전송 완료");
    }

    // -------------------- 받은 요청 목록 조회 --------------------
    // 현재 로그인한 유저에게 도착한(수신한) 친구 요청 리스트 반환
    @PostMapping("/received")
    public ResponseEntity<?> received(HttpServletRequest req) {
        return ResponseEntity.ok(requestService.getReceivedRequests(current(req)));
    }

    // -------------------- 보낸 요청 목록 조회 --------------------
    // 현재 로그인한 유저가 다른 사람에게 보낸 친구 요청 리스트 반환
    @PostMapping("/sents")
    public ResponseEntity<?> sent(HttpServletRequest req) {
        return ResponseEntity.ok(requestService.getSentRequests(current(req)));
    }

    // -------------------- 친구 요청 수락 --------------------
    // senderId(보낸 사람)를 받아서, 현재 로그인한 유저가 해당 요청을 수락하는 기능
    @PostMapping("/accept")
    public ResponseEntity<?> accept(@RequestBody FriendAcceptDto dto, HttpServletRequest req) {
        requestService.acceptRequest(dto.getSenderId(), current(req));
        return ResponseEntity.ok("수락 완료");
    }

    // -------------------- 친구 요청 거절 --------------------
    // senderId(보낸 사람)를 받아서, 현재 로그인한 유저가 해당 요청을 거절하는 기능
    @PostMapping("/reject")
    public ResponseEntity<?> reject(@RequestBody FriendRejectDto dto, HttpServletRequest req) {
        requestService.rejectRequest(dto.getSenderId(), current(req));
        return ResponseEntity.ok("거절 완료");
    }

    // -------------------- 내가 보낸 요청 취소 --------------------
    @PostMapping("/cancel")
    public ResponseEntity<?> cancel(
            @RequestBody FriendRequestCancelDto dto,
            HttpServletRequest req) {

        Long senderId = current(req);
        requestService.cancelSentRequest(senderId, dto.getReceiverId());

        return ResponseEntity.ok("보낸 친구 요청이 취소되었습니다.");
    }


}
