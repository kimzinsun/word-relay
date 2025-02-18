package com.wordrelay.server.controller;

import com.wordrelay.server.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@Slf4j
@RequiredArgsConstructor
public class GameWebSocketController {

  private final GameService gameService;
  private final SimpMessagingTemplate messagingTemplate;

  @MessageMapping("/game.connect")
  public void handleConnection(SimpMessageHeaderAccessor headerAccessor, Map<String, Object> payload) {
    String browserId = (String) payload.get("browserId");

    String nickname = gameService.handleUserConnection(browserId);

    log.info("👋👋👋 User connected: {}", nickname);

    messagingTemplate.convertAndSendToUser(
        headerAccessor.getSessionId(),
        "/queue/welcome",
        Map.of("nickname", nickname, "score", 0)
    );
  }
}
