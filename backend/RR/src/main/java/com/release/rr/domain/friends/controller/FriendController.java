package com.release.rr.domain.friends.controller;

import com.release.rr.domain.friends.dto.*;
import com.release.rr.domain.friends.service.FriendService;
import com.release.rr.global.security.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;
    private final JwtProvider jwtProvider;

    private Long getCurrentUser(HttpServletRequest request) {
        String token = jwtProvider.extractAccessTokenFromCookie(request);
        return jwtProvider.getUserId(token);
    }



    // -------------------- 친구 목록 (조회) --------------------
    @PostMapping
    public ResponseEntity<?> friends(HttpServletRequest request) {
        Long userId = getCurrentUser(request);
        return ResponseEntity.ok(friendService.getFriends(userId));
    }

    // -------------------- 친구 삭제 (닉네임 기반) --------------------
    @PostMapping("/delete")
    public ResponseEntity<?> delete(
            @RequestBody FriendDeleteDto dto,
            HttpServletRequest request) {

        Long userId = getCurrentUser(request);
        friendService.deleteFriend(userId, dto.getFriendNickname());

        return ResponseEntity.ok("친구 삭제 완료");
    }


    // -------------------- 친구 차단  --------------------

    @PostMapping("/block")
    public ResponseEntity<?> block(
            @RequestBody FriendBlockDto dto,
            HttpServletRequest request) {

        Long userId = getCurrentUser(request);
        friendService.blockFriend(userId, dto.getTargetNickname());

        return ResponseEntity.ok("차단 완료");
    }

    @PostMapping("/unblock")
    public ResponseEntity<?> unblock(
            @RequestBody FriendBlockDto dto,
            HttpServletRequest request) {

        Long userId = getCurrentUser(request);
        friendService.unblockFriend(userId, dto.getTargetNickname());

        return ResponseEntity.ok("차단 해제 완료");
    }



}



