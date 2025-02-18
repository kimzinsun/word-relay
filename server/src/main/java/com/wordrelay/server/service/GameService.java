package com.wordrelay.server.service;

import com.wordrelay.server.common.exception.CustomException;
import com.wordrelay.server.common.exception.ErrorCode;
import com.wordrelay.server.util.NicknameGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class GameService {

  private final RedisTemplate<String, String> redisTemplate;
  private static final String USER_SET_KEY = "game:users";
  private static final String USER_BROWSER_KEY = "game:user_browser";

  public GameService(@Qualifier("redisTemplateSession") RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public String handleUserConnection(String browserId) {
    if (browserId == null || browserId.isEmpty()) {
      throw new CustomException(ErrorCode.BROWSER_ID_MISSING.getCode(), ErrorCode.BROWSER_ID_MISSING.getMessage());
    }

    String nickname = redisTemplate.opsForValue().get(USER_BROWSER_KEY + browserId);

    if (nickname == null) {
      nickname = NicknameGenerator.generateRandomNickname();
      redisTemplate.opsForValue().set(USER_BROWSER_KEY + browserId, nickname);
      redisTemplate.opsForZSet().add(USER_SET_KEY, nickname, 0);
    }

    return nickname;
  }
}
