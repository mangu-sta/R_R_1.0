package com.release.rr.global.websocket;

import com.release.rr.global.redis.dao.UserSessionRedisDao;
import com.release.rr.global.security.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Arrays;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtProvider jwtProvider;
    private final UserSessionRedisDao userSessionRedisDao;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        // HTTPServletRequest 꺼내기
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }

        HttpServletRequest httpRequest = servletRequest.getServletRequest();
        String uri = httpRequest.getRequestURI();

        System.out.println("===== WS HANDSHAKE =====");
        System.out.println("URI = " + uri);
        System.out.println("Cookies = " + Arrays.toString(httpRequest.getCookies()));
        System.out.println("========================");

        /*
         * ==========================================================
         * ✅ 1️⃣ SockJS 내부 요청은 무조건 통과
         *  - info
         *  - iframe.html
         *  - xhr / xhr_streaming
         *  - jsonp
         *  - eventsource
         * ==========================================================
         */
        if (!uri.contains("/websocket")) {
            return true;
        }

        /*
         * ==========================================================
         * ⭐ 2️⃣ 여기부터 "진짜 WebSocket 연결"만 도착
         * ==========================================================
         */

        // ⭐ 3️⃣ 쿠키에서 access_token 추출
        String accessToken = null;
        Cookie[] cookies = httpRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                    break;
                }
            }
        }

        // ✅ 없으면 그냥 통과
        if (accessToken == null || accessToken.isEmpty()) {
            return true;
        }

        // ✅ 있으면 attributes에 넣어두기(선택)
        attributes.put("accessToken", accessToken);

        try {
            // 4️⃣ 토큰에서 userId 추출
            Long userId = jwtProvider.getUserId(accessToken);

            // ❌ 여기서 return false 하면 안 됨
            if (userId == null) {
                return true; // 🔥 변경
            }

            // 5️⃣ Claims 파싱 (실패해도 차단 ❌)
            Claims claims = jwtProvider.parseClaims(accessToken);

            // 6️⃣ 세션 attributes 저장 (옵션)
            attributes.put("userId", userId);
            attributes.put("nickname", claims.get("nickname", String.class));
            attributes.put("level", claims.get("level", Integer.class));

            return true;

        } catch (Exception e) {
            // ❌ 예외 발생해도 차단 금지
            return true; // 🔥 변경
        }

    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // 원본 유지
    }
}
