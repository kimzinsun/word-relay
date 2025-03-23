package com.wordrelay.server.service;

import com.wordrelay.server.common.response.ApiResponse;
import com.wordrelay.server.dto.WordMessage;
import com.wordrelay.server.mapper.WordMapper;
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
  private final WordMapper wordMapper;

  public GameService(
      @Qualifier("redisTemplateCurrentWord") RedisTemplate<String, String> redisTemplate,
      HangulUtil hangulUtil,
      WordMapper wordMapper) {
    this.redisTemplate = redisTemplate;
    this.hangulUtil = hangulUtil;
    this.wordMapper = wordMapper;
  }


  public ApiResponse<String> sendWord(WordMessage wordMessage) {
    String message = wordMessage.getWord();
    String currentWord = redisTemplate.opsForValue().get("currentWord");

    char lastChar = currentWord.charAt(currentWord.length() - 1);
    char firstChar = message.charAt(0);

    if (lastChar != firstChar) {
      log.info("Invalid word: {}", message);
      return ApiResponse.success("유효하지 않은 단어입니다.");
    }

    if(!hangulUtil.extractChoseong(message)) {
      log.info("Invalid word: {}", message);
      return ApiResponse.success("유효하지 않은 단어입니다.");
    }


      redisTemplate.opsForValue().set("currentWord", message);
      return ApiResponse.success("Success");
  }

}
