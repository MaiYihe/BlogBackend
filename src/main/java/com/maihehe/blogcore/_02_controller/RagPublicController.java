package com.maihehe.blogcore._02_controller;

import com.maihehe.blogcore._03_service.Impl.RagChatService;
import com.maihehe.blogcore._03_service.Impl.RagRateLimitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/rag")
public class RagPublicController {
    @Autowired
    private RagChatService ragChatService;
    @Autowired
    private RagRateLimitService ragRateLimitService;

    @PostMapping("/chat")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> chat(@RequestBody Map<String, Object> body,
                                                                       HttpServletRequest request) {
        try {
            String sessionId = body == null ? null : String.valueOf(body.getOrDefault("session_id", ""));
            String clientIp = resolveClientIp(request);
            String username = resolveUserId();
            String userId = "[" + clientIp + "," + username + "]";
            RagRateLimitService.LimitDecision decision = ragRateLimitService.check(userId);
            if (!decision.allowed()) {
                return CompletableFuture.completedFuture(
                        ResponseEntity.status(429).body(Map.of(
                                "status", "rate_limited",
                                "message", "rate limit exceeded",
                                "reason", decision.reason(),
                                "limit", decision.limit(),
                                "userId", userId
                        ))
                );
            }
            return ragChatService.chatAsync(body, sessionId, userId, clientIp)
                    .thenApply(ResponseEntity::ok)
                    .exceptionally(ex -> {
                        log.error("RAG chat failed", ex);
                        return ResponseEntity.status(500).body(Map.of(
                                "status", "error",
                                "message", ex.getMessage()
                        ));
                    });
        } catch (Exception e) {
            log.error("RAG chat failed", e);
            return CompletableFuture.completedFuture(ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            )));
        }
    }

    @GetMapping(value = "/chat/stream/{taskId}", produces = "text/event-stream")
    public SseEmitter chatStream(@PathVariable("taskId") String taskId,
                                 HttpServletRequest request) {
        String clientIp = resolveClientIp(request);
        String username = resolveUserId();
        String userId = "[" + clientIp + "," + username + "]";
        return ragChatService.streamTask(taskId, userId, clientIp);
    }

    @PostMapping("/chat/cancel/{taskId}")
    public ResponseEntity<Map<String, Object>> chatCancel(@PathVariable("taskId") String taskId) {
        try {
            Map<String, Object> res = ragChatService.cancelTask(taskId);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            log.error("RAG cancel failed", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    private String resolveUserId() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return "guest";
            }
            Object principal = auth.getPrincipal();
            if (principal == null) {
                return "guest";
            }
            if ("anonymousUser".equals(principal)) {
                return "guest";
            }
            return auth.getName();
        } catch (Exception e) {
            return "guest";
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            if (parts.length > 0) {
                return parts[0].trim();
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
