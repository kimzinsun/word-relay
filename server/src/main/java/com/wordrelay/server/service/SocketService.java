package com.wordrelay.server.service;

import com.wordrelay.server.common.exception.CustomException;
import com.wordrelay.server.common.exception.ErrorCode;
import com.wordrelay.server.util.NicknameGenerator;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SocketService {

  private final RedisTemplate<String, String> redisTemplate;
  private final SimpMessagingTemplate messagingTemplate;

  private final NicknameGenerator nicknameGenerator;

  private static final String USER_SET_KEY = "game:users";
  private static final String USER_BROWSER_KEY = "game:user_";

  public SocketService(@Qualifier("redisTemplateSession") RedisTemplate<String, String> redisTemplate,
      SimpMessagingTemplate messagingTemplate, NicknameGenerator nicknameGenerator) {
    this.redisTemplate = redisTemplate;
    this.messagingTemplate = messagingTemplate;
    this.nicknameGenerator = nicknameGenerator;
  }

  public String handleUserConnection(String browserId) {
    if (browserId == null || browserId.isEmpty()) {
      throw new CustomException(ErrorCode.BROWSER_ID_MISSING.getCode(), ErrorCode.BROWSER_ID_MISSING.getMessage());
    }


    String nickname = redisTemplate.opsForValue().get(USER_BROWSER_KEY + browserId);
    if (nickname == null || nickname.isEmpty()) {
      nickname = nicknameGenerator.generateRandomNickname();
      redisTemplate.opsForValue().set(USER_BROWSER_KEY + browserId, nickname);
      redisTemplate.opsForZSet().add(USER_SET_KEY, nickname, 0);
    }
    sendWelcomeMessage(browserId, nickname);

    return nickname;
  }

  private void sendWelcomeMessage(String browserId, String nickname) {
    Double score = redisTemplate.opsForZSet().score(USER_SET_KEY, nickname);
    int userScore = score.intValue();
    handleSendMessage(nickname, userScore, browserId);
  }

  public void handleSendMessage(String nickname, int score, String browserId) {
    Map<String, Object> welcomeMessage = Map.of(
        "message", "Welcome to the game, " + nickname + "! 🎉",
        "nickname", nickname,
        "score", score,
        "browserId", browserId
    );

    messagingTemplate.convertAndSend("/topic/welcome", welcomeMessage);
  }

}
