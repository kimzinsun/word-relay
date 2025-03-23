package com.wordrelay.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserService {

  private final RedisTemplate<String, String> redisTemplateLeaderBoard;

  public UserService(
      @Qualifier("redisTemplateLeaderBoard") RedisTemplate<String, String> redisTemplateLeaderBoard) {
    this.redisTemplateLeaderBoard = redisTemplateLeaderBoard;
  }

  public void addScore(String browserId, int score) {

    redisTemplateLeaderBoard.opsForZSet().incrementScore("game:users", browserId, score);

  }

}
