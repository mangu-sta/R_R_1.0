// com.release.rr.global.security.jwt.JwtProvider

package com.release.rr.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtProvider {

    private final Key secretKey;
    private final long accessExp;
    private final long refreshExp;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-exp-ms:1800000}") long accessExp,
            @Value("${jwt.refresh-exp-ms:1209600000}") long refreshExp
    ) {
        // HS256용 최소 32바이트 이상 필요
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExp = accessExp;
        this.refreshExp = refreshExp;
    }

    // -------- Access Token --------
    public String createAccessToken(Long userId, String nickname, Integer level) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claim("userId", userId)
                .claim("nickname", nickname)
                .claim("level", level)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + accessExp))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // -------- Refresh Token --------
    public String createRefreshToken(Long userId, String nickname, Integer level) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claim("userId", userId)
                .claim("nickname", nickname)
                .claim("level", level)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + refreshExp))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // -------- 공통 유틸 --------
    // ===== Claims 파싱 =====

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long getUserId(String token) {
        if (token == null || token.isEmpty()) return null;
        Claims claims = parseClaims(token);
        return claims.get("userId", Long.class);
    }


    /**
     * 쿠키에 저장된 Access Token(액세스 토큰)을 꺼내오는 메서드.
     *
     * 브라우저 → 서버 요청 시, HttpOnly 쿠키는 JS에서 접근이 불가능하므로
     * 서버 단에서 직접 HttpServletRequest 로 쿠키를 읽어야 한다.
     *
     * @param request 클라이언트 요청 객체
     * @return access_token 값 또는 null
     */
    public String extractAccessTokenFromCookie(HttpServletRequest request) {

        // 요청에 쿠키가 하나도 없으면 null 반환
        if (request.getCookies() == null) {
            return null;
        }

        // 모든 쿠키를 순회하면서 access_token 이름을 찾는다
        for (Cookie cookie : request.getCookies()) {

            // 쿠키 이름이 "access_token"이면 해당 값을 반환
            if ("access_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        // access_token 쿠키를 찾지 못하면 null
        return null;
    }

    /**
     * 쿠키에 저장된 Refresh Token(리프레시 토큰)을 꺼내오는 메서드.
     *
     * Refresh Token은 /api 경로에서만 전송되도록 path="/api" 로 설정되어 있으므로
     * 해당 요청에서만 쿠키가 전달된다.
     *
     * @param request 클라이언트 요청 객체
     * @return refresh_token 값 또는 null
     */
    public String extractRefreshTokenFromCookie(HttpServletRequest request) {

        // 쿠키가 없으면 null
        if (request.getCookies() == null) {
            return null;
        }

        // 쿠키 목록에서 refresh_token 찾기
        for (Cookie cookie : request.getCookies()) {
            if ("refresh_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }


    public boolean isAccessTokenValid(String token) {
        try {
            if (token == null || token.isEmpty()) return false;

            Claims claims = getClaims(token); // 파싱
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)   // 너의 JWT 서명키 변수명에 맞게 변경
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // (JwtProvider 내부 추가)
    public boolean isExpired(String token) {
        try {
            parseClaims(token);
            return false;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return true;
        }
    }



}
