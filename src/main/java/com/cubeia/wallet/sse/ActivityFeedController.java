package com.cubeia.wallet.sse;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/events")
public class ActivityFeedController {

    private final ActivityFeedService feedService;

    public ActivityFeedController(ActivityFeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        return feedService.subscribe();
    }

    @GetMapping("/connections")
    public ResponseEntity<Map<String, Integer>> connections() {
        return ResponseEntity.ok(Map.of("activeConnections", feedService.getActiveConnectionCount()));
    }
}
