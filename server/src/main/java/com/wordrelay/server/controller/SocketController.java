package com.wordrelay.server.controller;

import com.wordrelay.server.common.response.ApiResponse;
import com.wordrelay.server.service.SocketService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
@RequiredArgsConstructor
public class SocketController {
  private final SocketService socketService;

  @MessageMapping("/connect")
  public void handleConnection(Map<String, Object> payload) {
    String browserId = (String) payload.get("browserId");
    socketService.handleUserConnection(browserId);
  }


  // fallback용 API
  @GetMapping("/api/current-word")
  public ApiResponse<String> getCurrentWord() {
    String current = socketService.getCurrentWord();
    return ApiResponse.success(current);
  }


}
