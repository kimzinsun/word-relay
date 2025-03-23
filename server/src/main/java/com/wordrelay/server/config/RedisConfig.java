package com.wordrelay.server.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {
    private final RedisProperties redisProperties;

    @Bean(name = "redisConnectionFactoryLeaderBoard")
    public RedisConnectionFactory redisConnectionFactoryLeaderBoard() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();

        redisStandaloneConfiguration.setHostName(redisProperties.getHost());
        redisStandaloneConfiguration.setPort(redisProperties.getPort());
        redisStandaloneConfiguration.setDatabase(0);

        return new LettuceConnectionFactory(redisStandaloneConfiguration);
    }

    @Bean(name = "redisConnectionFactorySession")
    public RedisConnectionFactory redisConnectionFactorySession() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();

        redisStandaloneConfiguration.setHostName(redisProperties.getHost());
        redisStandaloneConfiguration.setPort(redisProperties.getPort());
        redisStandaloneConfiguration.setDatabase(1);

        return new LettuceConnectionFactory(redisStandaloneConfiguration);
    }

    @Bean(name = "redisTemplateLeaderBoard")
    public RedisTemplate<String, String> redisTemplateLeaderBoard(@Qualifier("redisConnectionFactoryLeaderBoard") RedisConnectionFactory redisConnectionFactoryLeaderBoard) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();

        redisTemplate.setConnectionFactory(redisConnectionFactoryLeaderBoard);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        return redisTemplate;
    }

    @Bean(name = "redisTemplateSession")
    public RedisTemplate<String, String> redisTemplateSession(@Qualifier("redisConnectionFactorySession") RedisConnectionFactory redisConnectionFactorySession) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();

        redisTemplate.setConnectionFactory(redisConnectionFactorySession);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        return redisTemplate;
    }

    @Bean(name = "redisConnectionFactoryCurrentWord")
    public RedisConnectionFactory redisConnectionFactoryCurrentWord() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();

        redisStandaloneConfiguration.setHostName(redisProperties.getHost());
        redisStandaloneConfiguration.setPort(redisProperties.getPort());
        redisStandaloneConfiguration.setDatabase(2);

        return new LettuceConnectionFactory(redisStandaloneConfiguration);
    }

    @Bean(name = "redisTemplateCurrentWord")
    public RedisTemplate<String, String> redisTemplateCurrentWord(@Qualifier("redisConnectionFactoryCurrentWord") RedisConnectionFactory redisConnectionFactoryCurrentWord) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();

        redisTemplate.setConnectionFactory(redisConnectionFactoryCurrentWord);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        return redisTemplate;
    }
}
