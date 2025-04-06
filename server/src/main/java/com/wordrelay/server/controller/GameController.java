package com.wordrelay.server.controller;

import com.wordrelay.server.common.response.ApiResponse;
import com.wordrelay.server.dto.WordMessage;
import com.wordrelay.server.dto.WordResultResponse;
import com.wordrelay.server.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class GameController {

  private final GameService gameService;

  @MessageMapping("/send")
  @SendTo("/game/word")
  public ApiResponse<WordResultResponse> sendWord(WordMessage wordMessage) {
    return gameService.sendWord(wordMessage);
  }

}
