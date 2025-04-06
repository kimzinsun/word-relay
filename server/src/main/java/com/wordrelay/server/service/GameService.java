package com.wordrelay.server.service;

import com.wordrelay.server.common.exception.ErrorCode;
import com.wordrelay.server.common.response.ApiResponse;
import com.wordrelay.server.common.response.SuccessCode;
import com.wordrelay.server.dto.WordMessage;
import com.wordrelay.server.dto.WordResultResponse;
import com.wordrelay.server.model.Word;
import com.wordrelay.server.util.HangulUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GameService {

  private static final String CURRENT_WORD = "currentWord";
  private final HangulUtil hangulUtil;
  private final UserService userService;
  private final RedisTemplate<String, String> redisTemplateCurrentWord;

  public GameService(
      @Qualifier("redisTemplateCurrentWord") RedisTemplate<String, String> redisTemplateCurrentWord,
      HangulUtil hangulUtil, UserService userService) {
    this.redisTemplateCurrentWord = redisTemplateCurrentWord;
    this.hangulUtil = hangulUtil;
    this.userService = userService;
  }


  public ApiResponse<WordResultResponse> sendWord(WordMessage wordMessage) {
    String message = wordMessage.getWord();
    String currentWord = redisTemplateCurrentWord.opsForValue().get(CURRENT_WORD);

    char lastChar = currentWord.charAt(currentWord.length() - 1);
    char firstChar = message.charAt(0);

    if (lastChar != firstChar) {
      return ApiResponse.error(ErrorCode.NOT_FOLLOWING_RULES.getCode(),
          ErrorCode.NOT_FOLLOWING_RULES.getMessage());
    }

    Word wordData = hangulUtil.getWord(message);

    if (wordData == null) {
      return ApiResponse.error(ErrorCode.INVALID_WORD.getCode(),
          ErrorCode.INVALID_WORD.getMessage());
    }

    if (Boolean.TRUE.equals(wordData.getWinningWord())) {

      // TODO: 랜덤 단어 선택
      redisTemplateCurrentWord.opsForValue().set(CURRENT_WORD, "시작");
      userService.addScore(wordMessage.getBrowserId(), 50);

      return ApiResponse.success(
          new WordResultResponse(true, "시작", SuccessCode.WORD_VALID.getMessage()));
    }
    userService.addScore(wordMessage.getBrowserId(), 10);
    redisTemplateCurrentWord.opsForValue().set(CURRENT_WORD, message);
    return ApiResponse.success(
        new WordResultResponse(true, message, SuccessCode.WORD_VALID.getMessage()));

  }
}
