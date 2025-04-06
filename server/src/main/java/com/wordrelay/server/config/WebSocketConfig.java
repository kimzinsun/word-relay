package com.wordrelay.server.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Value("${ALLOW_ORIGINS}")
  private String ALLOW_ORIGINS;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/game"); // 클라이언트 구독 경로
    config.setApplicationDestinationPrefixes("/app"); // 클라이언트 -> 서버 전송 경로
//    config.setUserDestinationPrefix("/user"); // 서버 -> 특정 유저 전송 경로
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws/game")
        .setAllowedOrigins(ALLOW_ORIGINS)
        .withSockJS();
  }


}
