package com.wordrelay.server.controller;

import com.wordrelay.server.common.response.ApiResponse;
import com.wordrelay.server.dto.WordMessage;
import com.wordrelay.server.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
@RequiredArgsConstructor
public class GameController {

  private final GameService gameService;

  @MessageMapping("/send.word")
  @SendTo("/topic/word")
  public ApiResponse<String> sendWord(WordMessage wordMessage) {
    log.info("Received word: {}", wordMessage.getWord());
    return gameService.sendWord(wordMessage);
//    gameService.handleWord(wordMessage.getMessage(), wordMessage.getBrowserId());
//    return ApiResponse.success("Success");
  }



}
