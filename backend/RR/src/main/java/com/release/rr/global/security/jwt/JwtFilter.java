// com.release.rr.global.security.jwt.JwtFilter

package com.release.rr.global.security.jwt;

import com.release.rr.global.redis.dao.UserSessionRedisDao;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserSessionRedisDao userSessionRedisDao;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // ======================= 기존 Access Token 추출 =======================
        String accessToken = extractTokenFromCookie(request);

        // ======================= 추가: Refresh Token도 같이 가져오기 =======================
        String refreshToken = jwtProvider.extractRefreshTokenFromCookie(request);

        // ======================= 1) Access Token 먼저 검사 =======================
        if (accessToken != null) {
            try {
                // Access Token 만료 여부 체크 (추가)
                if (!jwtProvider.isExpired(accessToken)) {

                    // 1) 토큰에서 유저 ID 추출
                    Long userId = jwtProvider.getUserId(accessToken);

                    // 2) Redis에 저장된 Access와 일치하는지 확인
                    String storedAccess = userSessionRedisDao.getAccessToken(userId);
                    if (storedAccess == null || !storedAccess.equals(accessToken)) {
                        throw new RuntimeException("로그인 세션이 만료되었습니다.");
                    }

                    // 3) Claims 파싱 후 SecurityContext에 저장
                    Claims claims = jwtProvider.parseClaims(accessToken);

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    claims,   // principal 자리에 Claims
                                    null,
                                    null      // 권한은 필요 시 나중에 추가
                            );

                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    filterChain.doFilter(request, response);
                    return;
                }

            } catch (ExpiredJwtException ex) {
                // Access Token 만료 시 → Refresh Token 처리로 넘어감
            } catch (Exception e) {
                // 기타 오류 → Refresh Token 검사로 넘어감
            }
        }

        // ======================= 2) Access Token 만료 → Refresh Token 검사 =======================
        if (refreshToken != null) {
            try {
                // Refresh Token 만료 여부 검사
                if (!jwtProvider.isExpired(refreshToken)) {

                    Long userId = jwtProvider.getUserId(refreshToken);
                    String storedRefresh = userSessionRedisDao.getRefreshToken(userId);

                    // Redis에 저장된 Refresh Token과 같은지 확인
                    if (storedRefresh != null && storedRefresh.equals(refreshToken)) {

                        // Refresh Claims 파싱
                        Claims claims = jwtProvider.parseClaims(refreshToken);

                        // ===== Access Token 재발급 =====
                        String newAccess = jwtProvider.createAccessToken(
                                userId,
                                claims.get("nickname", String.class),
                                claims.get("level", Integer.class)
                        );

                        // ⭐⭐⭐ 추가: Refresh Token도 재발급 ⭐⭐⭐
                        String newRefresh = jwtProvider.createRefreshToken(
                                userId,
                                claims.get("nickname", String.class),
                                claims.get("level", Integer.class)
                        );

                        // Redis Access Token 갱신
                        userSessionRedisDao.saveAccessToken(userId, newAccess);

                        // ⭐⭐⭐ Redis Refresh Token 갱신 (추가) ⭐⭐⭐
                        userSessionRedisDao.saveRefreshToken(userId, newRefresh);

                        // 새 Access Token을 쿠키에 저장
                        Cookie accessCookie = new Cookie("access_token", newAccess);
                        accessCookie.setHttpOnly(true);
                        accessCookie.setPath("/");
                        accessCookie.setMaxAge(60 * 30); // 30분
                        response.addCookie(accessCookie);

                        // ⭐⭐⭐ 새 Refresh Token을 쿠키에 저장 (추가) ⭐⭐⭐
                        Cookie refreshCookie = new Cookie("refresh_token", newRefresh);
                        refreshCookie.setHttpOnly(true);
                        refreshCookie.setPath("/api");  // 리프레시 토큰은 /api 경로에서만 사용
                        refreshCookie.setMaxAge(60 * 60 * 24 * 14); // 14일
                        response.addCookie(refreshCookie);

                        // SecurityContext 인증 주입
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(claims, null, null);

                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);

                        filterChain.doFilter(request, response);
                        return;
                    }
                }

            } catch (Exception e) {
                // Refresh Token도 문제 → 인증 실패
            }
        }

        // ======================= 3) Access / Refresh 둘 다 실패 → 인증 없음 =======================
        SecurityContextHolder.clearContext();
        filterChain.doFilter(request, response);
    }

    // ======================= 원본 유지: Access Token 쿠키 추출 =======================
    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if ("access_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
