package com.release.rr.domain.user.service;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.release.rr.domain.characters.repository.CharacterRepository;
import com.release.rr.domain.map.entity.MapEntity;
import com.release.rr.domain.map.repository.MapRepository;
import com.release.rr.domain.user.dto.*;
import com.release.rr.domain.user.entity.UserEntity;
import com.release.rr.domain.user.repository.UserRepository;
import com.release.rr.global.exception.CustomException;
import com.release.rr.global.exception.ErrorCode;
import com.release.rr.global.redis.dao.UserSessionRedisDao;
import com.release.rr.global.security.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class UserAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final UserSessionRedisDao userSessionRedisDao;
    private final CharacterRepository characterRepository;
    private final MapRepository mapRepository;

    // ==========================================================
    // 회원가입
    // ==========================================================
    @Transactional
    public ResponseEntity<?> signUp(SignUpRequestDto req, String ip) {

        validateSignUp(req);

        if (userRepository.existsByNickname(req.getNickname())) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        String encodedPw = passwordEncoder.encode(req.getPassword());

        // 1) 유저 생성
        UserEntity user = UserEntity.builder()
                .nickname(req.getNickname())
                .password(encodedPw)
                .ip(ip)
                .level(0)
                .build();

        userRepository.save(user);

        // 2) 유저 가입 후 자동으로 MAP 생성
        createMapForUser(user);

        return ResponseEntity.ok(new SignupResponseDto(true, "회원가입 성공"));
    }

    // ==========================================================
    // 로그인
    // ==========================================================
    public ResponseEntity<?> login(SignInRequestDto req, HttpServletResponse response) {

        UserEntity user = validateUser(req);

        boolean hasCharacter = characterRepository.findByUser(user).isPresent();

        String accessToken = jwtProvider.createAccessToken(
                user.getUserId(),
                user.getNickname(),
                user.getLevel());

        String refreshToken = jwtProvider.createRefreshToken(
                user.getUserId(),
                user.getNickname(),
                user.getLevel());

        userSessionRedisDao.saveAccessToken(user.getUserId(), accessToken);
        userSessionRedisDao.saveRefreshToken(user.getUserId(), refreshToken);

        setCookie(response, "access_token", accessToken, 60 * 30, "/");
        setCookie(response, "refresh_token", refreshToken, 60 * 60 * 24 * 14, "/api");

        return ResponseEntity.ok(new LoginResponseDto(true, hasCharacter));
    }

    // ==========================================================
    // 유저 검증 (로그인)
    // ==========================================================
    private UserEntity validateUser(SignInRequestDto req) {

        UserEntity entity = userRepository.findByNickname(req.getNickname())
                .orElseThrow(() -> new CustomException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(req.getPassword(), entity.getPassword())) {
            throw new CustomException(ErrorCode.LOGIN_FAILED);
        }

        return entity;
    }

    // ==========================================================
    // 회원가입 값 검증
    // ==========================================================
    private void validateSignUp(SignUpRequestDto req) {

        if (!req.getNickname().matches("^[A-Za-z0-9가-힣]{2,12}$")) {
            throw new CustomException(ErrorCode.INVALID_NICKNAME);
        }

        if (req.getPassword().length() < 6) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }
    }

    // ==========================================================
    // 로그아웃
    // ==========================================================
    public void logout(HttpServletRequest request, HttpServletResponse response) {

        String accessToken = extractAccessToken(request);

        if (accessToken != null) {
            try {
                Long userId = jwtProvider.getUserId(accessToken);
                userSessionRedisDao.deleteAllTokens(userId);
            } catch (Exception ignored) {
            }
        }

        clearCookie(response, "access_token", "/");
        clearCookie(response, "refresh_token", "/api");
    }

    // ==========================================================
    // 유저 삭제
    // ==========================================================
    @Transactional
    public void deleteUser(Long userId) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        characterRepository.deleteAllByUser(user);
        userRepository.delete(user);
        userSessionRedisDao.deleteAllTokens(userId);
    }

    // ==========================================================
    // Refresh Token 재발급
    // ==========================================================
    public ResponseEntity<?> refreshTokens(HttpServletRequest request, HttpServletResponse response) {

        String refreshToken = extractCookie(request, "refresh_token");

        if (refreshToken == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        try {
            Claims claims = jwtProvider.parseClaims(refreshToken);
            Long userId = claims.get("userId", Long.class);

            String stored = userSessionRedisDao.getRefreshToken(userId);
            if (stored == null || !stored.equals(refreshToken)) {
                throw new CustomException(ErrorCode.UNAUTHORIZED);
            }

            String nickname = claims.get("nickname", String.class);
            Integer level = claims.get("level", Integer.class);

            String newAccess = jwtProvider.createAccessToken(userId, nickname, level);
            String newRefresh = jwtProvider.createRefreshToken(userId, nickname, level);

            userSessionRedisDao.saveAccessToken(userId, newAccess);
            userSessionRedisDao.saveRefreshToken(userId, newRefresh);

            setCookie(response, "access_token", newAccess, 60 * 30, "/");
            setCookie(response, "refresh_token", newRefresh, 60 * 60 * 24 * 14, "/api");

            return ResponseEntity.ok("토큰 재발급 완료");

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }

    // ==========================================================
    // 내부 유틸
    // ==========================================================
    private String extractAccessToken(HttpServletRequest request) {
        if (request.getCookies() == null)
            return null;
        for (Cookie c : request.getCookies()) {
            if ("access_token".equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null)
            return null;
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    private void clearCookie(HttpServletResponse response, String name, String path) {

        ResponseCookie expired = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(false)
                .path(path)
                .sameSite("Lax")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", expired.toString());
    }

    private void setCookie(HttpServletResponse response,
            String name, String value,
            long maxAge, String path) {

        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(false)
                .path(path)
                .sameSite("Lax")
                .maxAge(maxAge)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    // 🔥 Map 생성 로직 분리 (createMapForUser)
    private void createMapForUser(UserEntity user) {

        // 1) nano_id 생성
        String nanoId = NanoIdUtils.randomNanoId(
                new SecureRandom(),
                NanoIdUtils.DEFAULT_ALPHABET,
                16);

        // 2) 맵 생성
        MapEntity map = MapEntity.builder()
                .owner(user)
                .nanoId(nanoId)
                .mapName(user.getNickname() + "의 맵") // 기본 이름
                .stage(0)
                .build();

        // 3) 저장
        mapRepository.save(map);
    }

    @Transactional
    public void saveKeyConfig(Long userId, String keyConfig) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.setKeyConfigJson(keyConfig);
        // 💡 save 안 해도 됨 (JPA 더티체킹)
    }

    // 키 불러오기
    public String getKeySetting(Long userId) {
        return userRepository.findById(userId)
                .map(UserEntity::getKeyConfigJson)
                .orElse(null);
    }

}
