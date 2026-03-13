package com.release.rr.domain.test.controller;

import com.release.rr.domain.characters.dto.CharacterResponseDto;
import com.release.rr.domain.characters.entity.CharacterEntity;
import com.release.rr.domain.characters.service.CharacterService;
import com.release.rr.global.redis.dao.LoginRedisDao;
import com.release.rr.global.security.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequiredArgsConstructor
public class test {

    CharacterService  characterService;
    JwtProvider  jwtProvider;

    @GetMapping("/api/me")
    public ResponseEntity<?> me(Authentication auth) {

        if (auth == null) {
            return ResponseEntity.status(401).body("인증 안됨");
        }

        Claims claims = (Claims) auth.getPrincipal();

        return ResponseEntity.ok(claims);
    }

    @GetMapping("/api/token")
    public ResponseEntity<?> showToken(HttpServletRequest request) {

        if (request.getCookies() == null) {
            return ResponseEntity.ok("쿠키 없음");
        }

        String access = null;
        String refresh = null;

        for (Cookie c : request.getCookies()) {
            if (c.getName().equals("access_token")) {
                access = c.getValue();
            }
            if (c.getName().equals("refresh_token")) {
                refresh = c.getValue();
            }
        }

        return ResponseEntity.ok(
                Map.of(
                        "access_token", access != null ? access : "없음",
                        "refresh_token", refresh != null ? refresh : "없음"
                )
        );
    }





    @DeleteMapping("/character/{id}")
    public ResponseEntity<?> deleteCharacter(
            @PathVariable Long id,
            @CookieValue("access_token") String token) {

        Long userId = jwtProvider.getUserId(token);

        characterService.deleteCharacter(id, userId);

        return ResponseEntity.ok("삭제 완료");
    }




}