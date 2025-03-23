package com.wordrelay.server.service;

import com.wordrelay.server.common.response.ApiResponse;
import com.wordrelay.server.dto.WordMessage;
import com.wordrelay.server.model.Word;
import com.wordrelay.server.util.HangulUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GameService {

  private final RedisTemplate<String, String> redisTemplate;
  private final HangulUtil hangulUtil;
  private final UserService userService;

  public GameService(
      @Qualifier("redisTemplateCurrentWord") RedisTemplate<String, String> redisTemplate,
      HangulUtil hangulUtil, UserService userService) {
    this.redisTemplate = redisTemplate;
    this.hangulUtil = hangulUtil;
    this.userService = userService;
  }


  public ApiResponse<String> sendWord(WordMessage wordMessage) {
    String message = wordMessage.getWord();
    String currentWord = redisTemplate.opsForValue().get("currentWord");

    char lastChar = currentWord.charAt(currentWord.length() - 1);
    char firstChar = message.charAt(0);

    if (lastChar != firstChar) {
      return ApiResponse.success("유효하지 않은 단어입니다.");
    }

    Word wordData = hangulUtil.getWord(message);

    if (wordData == null) {
      return ApiResponse.success("유효하지 않은 단어입니다.");
    }

    if (Boolean.TRUE.equals(wordData.getWinningWord())) {
      log.info("🎉 Winning word! 🎉");

      // TODO: 랜덤 단어 선택
      redisTemplate.opsForValue().set("currentWord", "시작");
      userService.addScore("browser_" + wordMessage.getBrowserId(), 50);

      return ApiResponse.success("Success");
    }

    log.info("👍 Correct word! 👍");

    userService.addScore(wordMessage.getBrowserId(), 10);

    redisTemplate.opsForValue().set("currentWord", message);
    return ApiResponse.success("Success");
  }


}
