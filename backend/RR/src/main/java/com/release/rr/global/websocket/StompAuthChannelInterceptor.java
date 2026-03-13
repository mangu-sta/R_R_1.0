/*
package com.release.rr.global.websocket;

import com.release.rr.global.redis.dao.UserSessionRedisDao;
import com.release.rr.global.security.jwt.JwtProvider;
import com.release.rr.global.exception.CustomException;
import com.release.rr.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.messaging.support.ChannelInterceptor;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;
    private final UserSessionRedisDao userSessionRedisDao;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(message);

        // ✅ STOMP CONNECT에서만 인증 수행
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String authHeader =
                    accessor.getFirstNativeHeader("Authorization");

            // 토큰 없음 → 연결 거부
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new CustomException(ErrorCode.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            // 토큰 만료 체크
            if (jwtProvider.isExpired(token)) {
                throw new CustomException(ErrorCode.UNAUTHORIZED);
            }

            Long userId = jwtProvider.getUserId(token);

            // Redis 세션 검증
            String storedToken =
                    userSessionRedisDao.getAccessToken(userId);

            if (!token.equals(storedToken)) {
                throw new CustomException(ErrorCode.UNAUTHORIZED);
            }

            // ✅ 인증 완료 → WebSocket 세션에 userId 저장
            accessor.getSessionAttributes().put("userId", userId);
        }

        return message;
    }
}
*/
