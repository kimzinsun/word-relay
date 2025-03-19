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
public class GameService {

  private final RedisTemplate<String, String> redisTemplate;
  private final SimpMessagingTemplate messagingTemplate;

  private static final String USER_SET_KEY = "game:users";
  private static final String USER_BROWSER_KEY = "game:user_";

  public GameService(@Qualifier("redisTemplateSession") RedisTemplate<String, String> redisTemplate,
      SimpMessagingTemplate messagingTemplate) {
    this.redisTemplate = redisTemplate;
    this.messagingTemplate = messagingTemplate;
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

    // Get current score
    Double score = redisTemplate.opsForZSet().score(USER_SET_KEY, nickname);
    int userScore = (score != null) ? score.intValue() : 0;

    // Send welcome message to the specific user
    Map<String, Object> welcomeMessage = Map.of(
        "message", "Welcome to the game, " + nickname + "! 🎉",
        "nickname", nickname,
        "score", userScore,
        "browserId", browserId
    );

    // Send to public channel with browser ID for filtering
    messagingTemplate.convertAndSend("/topic/welcome", welcomeMessage);

    // Also send a private message
    handleSendMessage("System", "Welcome to the game, " + nickname + "! You can start playing now.", nickname);

    return nickname;
  }

  public void handleSendMessage(String senderNickname, String message, String targetNickname) {
    if (targetNickname == null) {
      throw new CustomException(ErrorCode.USER_NOT_FOUND.getCode(), ErrorCode.USER_NOT_FOUND.getMessage());
    }

    Map<String, Object> messagePayload = Map.of(
        "nickname", senderNickname,
        "message", message
    );

    messagingTemplate.convertAndSendToUser(targetNickname, "/queue/private", messagePayload);
  }

  public void handleWordSubmission(String browserId, String word) {
    String nickname = redisTemplate.opsForValue().get(USER_BROWSER_KEY + browserId);

    if (nickname == null) {
      throw new CustomException(ErrorCode.USER_NOT_FOUND.getCode(), ErrorCode.USER_NOT_FOUND.getMessage());
    }

    // Game logic for word validation would go here

    // Example response
    handleSendMessage("System", "You submitted: " + word, nickname);
  }
}
