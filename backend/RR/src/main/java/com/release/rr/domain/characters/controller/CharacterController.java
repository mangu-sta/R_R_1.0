package com.release.rr.domain.characters.controller;


import com.release.rr.domain.characters.dto.CreateCharacterDto;
import com.release.rr.domain.characters.dto.CreateCharacterResponseDto;
import com.release.rr.domain.characters.dto.UserCharacterDto;
import com.release.rr.domain.characters.dto.UserCharacterRequestDto;
import com.release.rr.domain.characters.entity.CharacterEntity;
import com.release.rr.domain.characters.service.CharacterService;
import com.release.rr.domain.user.service.UserAuthService;
import com.release.rr.global.security.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api"
)
@RequiredArgsConstructor
public class CharacterController {

    final CharacterService characterService;
    final JwtProvider jwtProvider;

    @PostMapping("/character/create")
    public ResponseEntity<?> createCharacter(
            @RequestBody CreateCharacterDto req,
            @CookieValue("access_token") String token
    ) {

        Long userId = jwtProvider.getUserId(token);   // ← ★ 핵심 변경

        CharacterEntity.Job job = CharacterEntity.Job.valueOf(req.getJob());

        CharacterEntity created = characterService.createCharacter(
                userId,     // ← 쿠키에서 가져온 유저로 생성
                job
        );

        CreateCharacterResponseDto response =
                new CreateCharacterResponseDto(created.getJob().name());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/character")
    public UserCharacterDto getUserCharacter(@RequestBody UserCharacterRequestDto req,
                                             Authentication auth) {

        // 1) JWT에서 본인 userId 가져오기
        Claims claims = (Claims) auth.getPrincipal();
        Long myUserId = claims.get("userId", Long.class);

        // 2) 요청에서 userId가 null이 아니면 → 다른 유저 조회
        //    요청 userId가 null이면 → 본인 조회
        Long targetUserId = (req.getUserId() != null) ? req.getUserId() : myUserId;

        return characterService.getUserCharacter(targetUserId);
    }




}
