package com.wordrelay.server.controller;

import com.wordrelay.server.service.SSEService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/sse")
public class SSEController {
    private static final Logger logger = LoggerFactory.getLogger(SSEController.class);

    private final SSEService sseService;

    public SSEController(SSEService sseService) {
        this.sseService = sseService;
    }

    /**
     * SSE 연결을 설정합니다.
     */
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@RequestParam String browserId) {
        logger.info("SSE connection request received for browserId: {}", browserId);
        return sseService.createEmitter(browserId);
    }

    /**
     * 활성 SSE 연결 상태를 확인합니다.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(sseService.getActiveConnections());
    }

    /**
     * 특정 사용자에게 점수를 전송합니다.
     */
    @PostMapping("/score/{browserId}")
    public ResponseEntity<String> sendScoreToUser(@PathVariable String browserId) {
        logger.info("Manually sending score to browserId: {}", browserId);
        sseService.sendScoreToUser(browserId);
        return ResponseEntity.ok("Score event sent to user: " + browserId);
    }

    /**
     * 모든 클라이언트에게 점수를 브로드캐스트합니다.
     */
    @PostMapping("/broadcast/scores")
    public ResponseEntity<String> broadcastScores() {
        logger.info("Manually broadcasting scores to all clients");
        sseService.broadcastScores();
        return ResponseEntity.ok("Score broadcast initiated");
    }

    /**
     * 특정 사용자에게 커스텀 이벤트를 전송합니다.
     */
    @PostMapping("/event/{browserId}")
    public ResponseEntity<String> sendEventToUser(
        @PathVariable String browserId,
        @RequestParam String eventName,
        @RequestBody Object data) {
        logger.info("Sending custom event '{}' to browserId: {}", eventName, browserId);
        boolean sent = sseService.sendEvent(browserId, eventName, data);

        if (sent) {
            return ResponseEntity.ok("Event sent to user: " + browserId);
        } else {
            return ResponseEntity.ok("User not connected: " + browserId);
        }
    }

    /**
     * 모든 클라이언트에게 커스텀 이벤트를 브로드캐스트합니다.
     */
    @PostMapping("/broadcast/event")
    public ResponseEntity<String> broadcastEvent(
        @RequestParam String eventName,
        @RequestBody Object data) {
        logger.info("Broadcasting custom event '{}' to all clients", eventName);
        sseService.broadcastEvent(eventName, data);
        return ResponseEntity.ok("Event broadcast initiated");
    }

    /**
     * 특정 사용자의 연결을 종료합니다.
     */
    @DeleteMapping("/disconnect/{browserId}")
    public ResponseEntity<String> disconnectUser(@PathVariable String browserId) {
        logger.info("Disconnecting user: {}", browserId);
        sseService.removeEmitter(browserId);
        return ResponseEntity.ok("User disconnected: " + browserId);
    }
}