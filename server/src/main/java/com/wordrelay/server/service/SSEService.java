package com.wordrelay.server.service;

import com.wordrelay.server.repository.RankingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SSEService {
    private static final Logger logger = LoggerFactory.getLogger(SSEService.class);

    private final RankingRepository rankingRepository;

    // SSE 연결 관리
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<String, Long> lastActivityTime = new ConcurrentHashMap<>();

    // 상수 정의
    private static final long SSE_TIMEOUT = 5 * 60 * 1000L; // 5분
    private static final long HEARTBEAT_INTERVAL = 30 * 1000L; // 30초
    private static final long CLEANUP_INTERVAL = 2 * 60 * 1000L; // 2분
    private static final long STALE_THRESHOLD = 3 * 60 * 1000L; // 3분

    public SSEService(RankingRepository rankingRepository) {
        this.rankingRepository = rankingRepository;
        logger.info("SSEService initialized");
    }

    /**
     * 새 SSE 연결을 생성합니다.
     */
    public SseEmitter createEmitter(String browserId) {
        logger.info("Creating SSE emitter for browserId: {}", browserId);

        // 기존 연결 정리
        removeEmitter(browserId);

        SseEmitter emitter = createSseEmitter(browserId);

        // 초기 데이터 전송
        try {
            sendConnectEvent(emitter, browserId);
            sendScoreToUser(browserId);
        } catch (IOException e) {
            logger.error("Error sending initial data to browserId: {}", browserId, e);
            cleanupEmitter(browserId);
        }

        return emitter;
    }

    /**
     * SseEmitter 인스턴스를 생성하고 콜백을 설정합니다.
     */
    private SseEmitter createSseEmitter(String browserId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 콜백 설정
        emitter.onCompletion(() -> {
            logger.info("SSE connection completed for browserId: {}", browserId);
            cleanupEmitter(browserId);
        });

        emitter.onTimeout(() -> {
            logger.info("SSE connection timed out for browserId: {}", browserId);
            cleanupEmitter(browserId);
        });

        emitter.onError(e -> {
            logger.error("SSE connection error for browserId: {}", browserId, e);
            cleanupEmitter(browserId);
        });

        // 이미터 저장
        emitters.put(browserId, emitter);
        lastActivityTime.put(browserId, System.currentTimeMillis());

        return emitter;
    }

    /**
     * 연결 이벤트를 전송합니다.
     */
    private void sendConnectEvent(SseEmitter emitter, String browserId) throws IOException {
        emitter.send(SseEmitter.event()
            .name("connect")
            .data("Connected for user: " + browserId));
    }

    /**
     * 이미터와 관련된 리소스를 정리합니다.
     */
    private void cleanupEmitter(String browserId) {
        emitters.remove(browserId);
        lastActivityTime.remove(browserId);
    }

    /**
     * 이미터를 제거합니다.
     */
    public void removeEmitter(String browserId) {
        SseEmitter emitter = emitters.remove(browserId);
        if (emitter != null) {
            logger.info("Removing existing emitter for browserId: {}", browserId);
            emitter.complete();
            lastActivityTime.remove(browserId);
        }
    }

    /**
     * 해당 브라우저 ID의 이미터를 찾습니다.
     */
    private SseEmitter findEmitter(String browserId) {
        return emitters.get(browserId);
    }

    /**
     * 활성 연결 정보를 반환합니다.
     */
    public Map<String, Object> getActiveConnections() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("activeConnections", emitters.size());
        status.put("connectedClients", emitters.keySet());
        return status;
    }

    /**
     * 모든 클라이언트에 점수를 브로드캐스트합니다.
     */
    public void broadcastScores() {
        logger.info("Broadcasting scores to all clients. Active connections: {}", emitters.size());

        if (emitters.isEmpty()) {
            logger.debug("No active SSE connections for broadcast");
            return;
        }

        emitters.forEach((browserId, emitter) -> {
            sendScoreToUser(browserId);
        });
    }

    /**
     * 특정 사용자에게 점수를 전송합니다.
     */
    public void sendScoreToUser(String browserId) {
        SseEmitter emitter = findEmitter(browserId);
        if (emitter != null) {
            try {
                Double score = rankingRepository.getScoreByBrowserId(browserId);
                logger.debug("Sending score {} to browserId: {}", score, browserId);
                emitter.send(SseEmitter.event()
                    .name("score")
                    .data(score));
                updateLastActivityTime(browserId);
            } catch (IOException e) {
                logger.error("Error sending score to browserId: {}", browserId, e);
                cleanupEmitter(browserId);
            }
        } else {
            logger.debug("Cannot send score - no emitter found for browserId: {}", browserId);
        }
    }

    /**
     * 마지막 활동 시간을 업데이트합니다.
     */
    private void updateLastActivityTime(String browserId) {
        lastActivityTime.put(browserId, System.currentTimeMillis());
    }

    /**
     * 하트비트 메커니즘을 통한 연결 유지
     */
    @Scheduled(fixedRate = HEARTBEAT_INTERVAL)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) {
            return;
        }

        logger.debug("Sending heartbeat to {} connected clients", emitters.size());

        emitters.forEach((browserId, emitter) -> {
            try {
                logger.trace("Sending heartbeat to browserId: {}", browserId);
                emitter.send(SseEmitter.event()
                    .name("heartbeat")
                    .data("ping"));
                updateLastActivityTime(browserId);
            } catch (IOException e) {
                logger.error("Error sending heartbeat to browserId: {}", browserId, e);
                cleanupEmitter(browserId);
            }
        });
    }

    /**
     * 오래된 연결 정리
     */
    @Scheduled(fixedRate = CLEANUP_INTERVAL)
    public void cleanupStaleConnections() {
        if (emitters.isEmpty()) {
            return;
        }

        logger.debug("Checking for stale connections");
        long now = System.currentTimeMillis();

        emitters.entrySet().removeIf(entry -> {
            String browserId = entry.getKey();
            Long lastActivity = lastActivityTime.get(browserId);

            if (lastActivity == null || now - lastActivity > STALE_THRESHOLD) {
                logger.info("Removing stale connection for browserId: {}", browserId);
                SseEmitter emitter = entry.getValue();
                emitter.complete();
                lastActivityTime.remove(browserId);
                return true;
            }
            return false;
        });
    }

    /**
     * 사용자에게 커스텀 이벤트를 전송합니다.
     */
    public boolean sendEvent(String browserId, String eventName, Object data) {
        SseEmitter emitter = findEmitter(browserId);
        if (emitter != null) {
            try {
                logger.debug("Sending custom event '{}' to browserId: {}", eventName, browserId);
                emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
                updateLastActivityTime(browserId);
                return true;
            } catch (IOException e) {
                logger.error("Error sending custom event to browserId: {}", browserId, e);
                cleanupEmitter(browserId);
            }
        }
        return false;
    }

    /**
     * 모든 클라이언트에게 커스텀 이벤트를 브로드캐스트합니다.
     */
    public void broadcastEvent(String eventName, Object data) {
        logger.info("Broadcasting custom event '{}' to all clients", eventName);

        if (emitters.isEmpty()) {
            logger.debug("No active SSE connections for broadcast");
            return;
        }

        emitters.forEach((browserId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
                updateLastActivityTime(browserId);
            } catch (IOException e) {
                logger.error("Error broadcasting event to browserId: {}", browserId, e);
                cleanupEmitter(browserId);
            }
        });
    }
}