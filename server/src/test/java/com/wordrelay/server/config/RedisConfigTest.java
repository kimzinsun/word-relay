package com.wordrelay.server.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RedisConfigTest {

    @Autowired
    private RedisProperties redisProperties;

    @Autowired
    @Qualifier("redisConnectionFactoryLeaderBoard")
    private RedisConnectionFactory redisConnectionFactoryLeaderBoard;

    @Autowired
    @Qualifier("redisConnectionFactorySession")
    private RedisConnectionFactory redisConnectionFactorySession;

    @Autowired
    @Qualifier("redisTemplateLeaderBoard")
    private RedisTemplate<String, String> redisTemplateLeaderBoard;

    @Autowired
    @Qualifier("redisTemplateSession")
    private RedisTemplate<String, String> redisTemplateSession;

    @Test
    @DisplayName("리더보드용 RedisConnectionFactory가 정상적으로 생성되는지 테스트")
    void redisConnectionFactoryLeaderBoardTest() {
        assertThat(redisConnectionFactoryLeaderBoard).isNotNull();
        assertThat(redisConnectionFactoryLeaderBoard)
                .isInstanceOf(RedisConnectionFactory.class);
    }

    @Test
    @DisplayName("세션용 RedisConnectionFactory가 정상적으로 생성되는지 테스트")
    void redisConnectionFactorySessionTest() {
        assertThat(redisConnectionFactorySession).isNotNull();
        assertThat(redisConnectionFactorySession)
                .isInstanceOf(RedisConnectionFactory.class);
    }

    @Test
    @DisplayName("리더보드용 RedisTemplate 설정 테스트")
    void redisTemplateLeaderBoardTest() {
        assertThat(redisTemplateLeaderBoard).isNotNull();
        assertThat(redisTemplateLeaderBoard.getKeySerializer())
                .isInstanceOf(StringRedisSerializer.class);
        assertThat(redisTemplateLeaderBoard.getValueSerializer())
                .isInstanceOf(StringRedisSerializer.class);
    }

    @Test
    @DisplayName("세션용 RedisTemplate 설정 테스트")
    void redisTemplateSessionTest() {
        assertThat(redisTemplateSession).isNotNull();
        assertThat(redisTemplateSession.getKeySerializer())
                .isInstanceOf(StringRedisSerializer.class);
        assertThat(redisTemplateSession.getValueSerializer())
                .isInstanceOf(StringRedisSerializer.class);
    }

    @Test
    @DisplayName("리더보드와 세션의 RedisConnectionFactory가 서로 다른 인스턴스인지 테스트")
    void differentConnectionFactoriesTest() {
        assertThat(redisConnectionFactoryLeaderBoard)
                .isNotSameAs(redisConnectionFactorySession);
    }

    @Test
    @DisplayName("리더보드와 세션의 RedisTemplate이 서로 다른 인스턴스인지 테스트")
    void differentRedisTemplatesTest() {
        assertThat(redisTemplateLeaderBoard)
                .isNotSameAs(redisTemplateSession);
    }

    @Test
    @DisplayName("리더보드용 Redis가 데이터베이스 0번으로 설정되었는지 테스트")
    void leaderBoardDatabaseConfigurationTest() {
        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) redisConnectionFactoryLeaderBoard;
        assertThat(lettuceFactory.getDatabase()).isEqualTo(0);
    }

    @Test
    @DisplayName("세션용 Redis가 데이터베이스 1번으로 설정되었는지 테스트")
    void sessionDatabaseConfigurationTest() {
        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) redisConnectionFactorySession;
        assertThat(lettuceFactory.getDatabase()).isEqualTo(1);
    }

    @Test
    @DisplayName("리더보드와 세션의 Redis 데이터베이스가 서로 다른지 테스트")
    void differentDatabaseConfigurationTest() {
        LettuceConnectionFactory leaderBoardFactory = (LettuceConnectionFactory) redisConnectionFactoryLeaderBoard;
        LettuceConnectionFactory sessionFactory = (LettuceConnectionFactory) redisConnectionFactorySession;

        assertThat(leaderBoardFactory.getDatabase())
                .isNotEqualTo(sessionFactory.getDatabase());
    }

    @Test
    @DisplayName("Redis 서버 설정 테스트")
    void redisServerConfigurationTest() {
        LettuceConnectionFactory leaderBoardFactory = (LettuceConnectionFactory) redisConnectionFactoryLeaderBoard;
        LettuceConnectionFactory sessionFactory = (LettuceConnectionFactory) redisConnectionFactorySession;

        // 리더보드 Redis 설정 확인
        assertThat(leaderBoardFactory.getHostName()).isEqualTo(redisProperties.getHost());
        assertThat(leaderBoardFactory.getPort()).isEqualTo(redisProperties.getPort());

        // 세션 Redis 설정 확인
        assertThat(sessionFactory.getHostName()).isEqualTo(redisProperties.getHost());
        assertThat(sessionFactory.getPort()).isEqualTo(redisProperties.getPort());
    }
}