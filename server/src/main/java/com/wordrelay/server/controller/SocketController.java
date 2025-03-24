package com.wordrelay.server.controller;

import com.wordrelay.server.service.GameService;
import com.wordrelay.server.service.SocketService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
@RequiredArgsConstructor
public class SocketController {
  private final SocketService socketService;

  @MessageMapping("/game.connect")
  public void handleConnection(SimpMessageHeaderAccessor headerAccessor, Map<String, Object> payload) {
    String browserId = (String) payload.get("browserId");
    String nickname = socketService.handleUserConnection(browserId);
    headerAccessor.getSessionAttributes().put("username", nickname);

  }

}
