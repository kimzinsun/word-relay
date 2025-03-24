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

  private final RedisTemplate<String, String> redisTemplateSession;

  private final RedisTemplate<String, String> redisTemplateLeaderBoard;
  private final SimpMessagingTemplate messagingTemplate;

  private final NicknameGenerator nicknameGenerator;

  private static final String USER_SET_KEY = "game:users";
  private static final String USER_BROWSER_KEY = "game:user_";

  public SocketService(
      @Qualifier("redisTemplateSession") RedisTemplate<String, String> redisTemplateSession,
      @Qualifier("redisTemplateLeaderBoard") RedisTemplate<String, String> redisTemplateLeaderBoard,
      SimpMessagingTemplate messagingTemplate, NicknameGenerator nicknameGenerator) {
    this.redisTemplateSession = redisTemplateSession;
    this.redisTemplateLeaderBoard = redisTemplateLeaderBoard;
    this.messagingTemplate = messagingTemplate;
    this.nicknameGenerator = nicknameGenerator;
  }

  public String handleUserConnection(String browserId) {
    if (browserId == null || browserId.isEmpty()) {
      throw new CustomException(ErrorCode.BROWSER_ID_MISSING.getCode(),
          ErrorCode.BROWSER_ID_MISSING.getMessage());
    }

    String nickname = redisTemplateSession.opsForValue().get(USER_BROWSER_KEY + browserId);
    if (nickname == null || nickname.isEmpty()) {
      nickname = nicknameGenerator.generateRandomNickname();
      redisTemplateSession.opsForValue().set(USER_BROWSER_KEY + browserId, nickname);
      redisTemplateLeaderBoard.opsForZSet().add(USER_SET_KEY, browserId, 0);
    }
    sendWelcomeMessage(browserId, nickname);

    return nickname;
  }

  private void sendWelcomeMessage(String browserId, String nickname) {
    Double score = redisTemplateLeaderBoard.opsForZSet().score(USER_SET_KEY, browserId);
    int userScore = score.intValue();
    handleSendMessage(nickname);
  }

  public void handleSendMessage(String nickname) {
    Map<String, Object> welcomeMessage = Map.of("message",
        "Welcome to the game, " + nickname + "! 🎉", "nickname", nickname);

    messagingTemplate.convertAndSend("/topic/welcome", welcomeMessage);
  }

}
