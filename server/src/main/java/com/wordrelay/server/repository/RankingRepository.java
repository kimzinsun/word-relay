package com.wordrelay.server.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Objects;

@Repository
public class RankingRepository {
    private static final String USER_SET_KEY = "game:users";
    private static final Double INITIAL_SCORE = 0d;

    private final RedisTemplate<String, String> redisTemplateRankings;

    public RankingRepository(RedisTemplate<String, String> redisTemplateRankings) {
        this.redisTemplateRankings = redisTemplateRankings;
    }

    public void createRankByBrowserId(String browserId) {
        redisTemplateRankings.opsForZSet().add(USER_SET_KEY, browserId, INITIAL_SCORE);
    }

    public void addScoreByBrowserId(String browserId, int score) {
        redisTemplateRankings.opsForZSet().incrementScore(USER_SET_KEY, browserId, score);
    }

    public Double getScoreByBrowserId(String browserId) {
        return redisTemplateRankings.opsForZSet().score(USER_SET_KEY, browserId);
    }

    public Double getRankByBrowserId(String browserId) {
        return Objects.requireNonNull(redisTemplateRankings.opsForZSet().rank(USER_SET_KEY, browserId)).doubleValue();
    }
}
