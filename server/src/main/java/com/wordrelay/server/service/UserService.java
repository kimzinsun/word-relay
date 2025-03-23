package com.wordrelay.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserService {

  private final RedisTemplate<String, String> redisTemplate;

  public UserService(
      @Qualifier("redisTemplateLeaderBoard") RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void addScore(String browserId, int score) {

    log.info("Adding score to user: {}", browserId);
    log.info("Score: {}", score);
    redisTemplate.opsForZSet().incrementScore("game:users", browserId, score);
    log.info("##########" + redisTemplate.opsForZSet().score("game:users", browserId));

  }

}
