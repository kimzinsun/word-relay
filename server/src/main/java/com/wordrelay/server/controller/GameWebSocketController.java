package com.wordrelay.server.controller;

import com.wordrelay.server.service.GameService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
@RequiredArgsConstructor
public class GameWebSocketController {

  private final GameService gameService;

  @MessageMapping("/game.connect")
  public void handleConnection(SimpMessageHeaderAccessor headerAccessor, Map<String, Object> payload) {
    String browserId = (String) payload.get("browserId");
    String nickname = gameService.handleUserConnection(browserId);
    headerAccessor.getSessionAttributes().put("username", nickname);
    log.info("User connected: " + nickname);

  }





}
