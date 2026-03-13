package com.release.rr.global.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
//    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/lobby")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns("*"
                )
                .withSockJS();

        // SockJS 안 쓸 거면 .withSockJS() 제거하고 cors만 남겨도 됨
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 서버 → 클라이언트로 보내는 prefix
        registry.enableSimpleBroker("/topic");

        // 클라이언트 → 서버로 보내는 prefix
        registry.setApplicationDestinationPrefixes("/app");
    }

   /* @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }*/

}
