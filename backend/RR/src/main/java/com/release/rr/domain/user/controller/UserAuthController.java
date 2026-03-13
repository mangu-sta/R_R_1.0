// com.release.rr.domain.user.controller.UserAuthController

package com.release.rr.domain.user.controller;

import com.release.rr.domain.user.dto.*;
import com.release.rr.domain.user.service.UserAuthService;
import com.release.rr.global.exception.CustomException;
import com.release.rr.global.exception.ErrorCode;
import com.release.rr.global.redis.dao.UserSessionRedisDao;
import com.release.rr.global.security.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api")
@RequiredArgsConstructor
public class UserAuthController {

    private final UserAuthService userAuthService;          // MariaDB
    private final JwtProvider jwtProvider;                  // JWT 생성/파싱
    private final UserSessionRedisDao userSessionRedisDao;  // Redis 세션

    // ------------------ 회원가입 ------------------
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody SignUpRequestDto req, HttpServletRequest http) {

        String clientIp = http.getRemoteAddr();
        return userAuthService.signUp(req, clientIp);
    }

    // ------------------ 로그인 ------------------
    @PostMapping("/signin")
    public ResponseEntity<?> signIn(@RequestBody SignInRequestDto req,
                                    HttpServletResponse response) {

        return userAuthService.login(req, response);
    }


    // ------------------ Access / Refresh 재발급 ------------------
    @PostMapping("/auth/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request,
                                     HttpServletResponse response) {

        return userAuthService.refreshTokens(request, response);
    }


    // ------------------ 로그아웃 ------------------
    @PostMapping("/signout")
    public ResponseEntity<?> signOut(HttpServletRequest request,
                                     HttpServletResponse response) {

        userAuthService.logout(request, response); // ⭐ 서비스에게 위임

        return ResponseEntity.ok("로그아웃 완료");
    }

    //------------------ 유저 삭제 ------------------------------
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {

        userAuthService.deleteUser(userId);

        return ResponseEntity.ok("유저 및 모든 캐릭터 삭제 완료");
    }


    // ------------------ Access Token 검증 ------------------
    @PostMapping("/auth/verify")
    public ResponseEntity<?> verifyToken(HttpServletRequest request) {

        boolean valid = jwtProvider.isAccessTokenValid(
                jwtProvider.extractAccessTokenFromCookie(request)
        );

        // 토큰이 유효한 경우 userId도 같이 보냄
        if (valid) {
            Long userId = jwtProvider.getUserId(
                    jwtProvider.extractAccessTokenFromCookie(request)
            );

            return ResponseEntity.ok(new TokenVerifyResponse(true, userId));
        }

        return ResponseEntity.ok(new TokenVerifyResponse(false, null));
    }

    @PostMapping("/key-config")
    public ResponseEntity<Void> saveKeyConfig(
            HttpServletRequest request,
            @RequestBody KeyConfigSaveRequest req
    ) {
        String accessToken = jwtProvider.extractAccessTokenFromCookie(request);

        if (accessToken == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = jwtProvider.getUserId(accessToken);

        if (userId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        userAuthService.saveKeyConfig(userId, req.getKeyConfig());
        return ResponseEntity.ok().build();
    }



}
