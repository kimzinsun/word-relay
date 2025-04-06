package com.wordrelay.server.service;

import com.wordrelay.server.common.exception.CustomException;
import com.wordrelay.server.common.exception.ErrorCode;
import com.wordrelay.server.dto.ConnectResponse;
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
  private final RedisTemplate<String, String> redisTemplateCurrentWord;
  private final SimpMessagingTemplate messagingTemplate;

  private final NicknameGenerator nicknameGenerator;

  private static final String USER_SET_KEY = "game:users";
  private static final String CURRENT_WORD = "currentWord";


  public SocketService(
      @Qualifier("redisTemplateSession") RedisTemplate<String, String> redisTemplateSession,
      @Qualifier("redisTemplateLeaderBoard") RedisTemplate<String, String> redisTemplateLeaderBoard,
      @Qualifier("redisTemplateCurrentWord") RedisTemplate<String, String> redisTemplateCurrentWord,
      SimpMessagingTemplate messagingTemplate, NicknameGenerator nicknameGenerator) {
    this.redisTemplateSession = redisTemplateSession;
    this.redisTemplateLeaderBoard = redisTemplateLeaderBoard;
    this.redisTemplateCurrentWord = redisTemplateCurrentWord;
    this.messagingTemplate = messagingTemplate;
    this.nicknameGenerator = nicknameGenerator;
  }

  public void handleUserConnection(String browserId) {
    if (browserId == null || browserId.isEmpty()) {
      throw new CustomException(ErrorCode.BROWSER_ID_MISSING.getCode(),
          ErrorCode.BROWSER_ID_MISSING.getMessage());
    }

    String nickname = redisTemplateSession.opsForValue().get(browserId);
    if (nickname == null || nickname.isEmpty()) {
      nickname = nicknameGenerator.generateRandomNickname();
      redisTemplateSession.opsForValue().set(browserId, nickname);
      redisTemplateLeaderBoard.opsForZSet().add(USER_SET_KEY, browserId, 0);
    }
    sendWelcomeMessage(browserId, nickname);
  }

  private void sendWelcomeMessage(String browserId, String nickname) {
    String currentWord = redisTemplateCurrentWord.opsForValue().get("currentWord");
    Map<String, Object> currentWordInfo = Map.of("currentWord", currentWord);
    messagingTemplate.convertAndSend("/topic/userInfo", new ConnectResponse(nickname, browserId));
    messagingTemplate.convertAndSend("/topic/currentWord", currentWordInfo);
  }


  public String getCurrentWord() {
    return redisTemplateCurrentWord.opsForValue().get(CURRENT_WORD);
  }

}
