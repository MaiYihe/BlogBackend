package com.maihehe.blogcore._03_service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Slf4j
@Service
public class RagRateLimitService {

    private final StringRedisTemplate redisTemplate;

    @Value("${rag.chat.daily-limit:30}")
    private int dailyLimit;

    @Value("${rag.chat.limit-enabled:true}")
    private boolean enabled;

    @Value("${rag.chat.qps-enabled:true}")
    private boolean qpsEnabled;

    @Value("${rag.chat.qps-interval-ms:1000}")
    private long qpsIntervalMs;

    public RagRateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public record LimitDecision(boolean allowed, String reason, Integer limit) {}

    public LimitDecision check(String userId) {
        String uid = normalize(userId);
        try {
            if (qpsEnabled && qpsIntervalMs > 0) {
                if (!allowInterval(uid)) {
                    return new LimitDecision(false, "qps", 1);
                }
            }
            if (enabled && dailyLimit > 0) {
                if (!allowDaily(uid)) {
                    return new LimitDecision(false, "daily", dailyLimit);
                }
            }
            return new LimitDecision(true, null, null);
        } catch (Exception e) {
            log.warn("RAG rate limit check failed, allow by default: userId={}, err={}", uid, e.toString());
            return new LimitDecision(true, null, null);
        }
    }

    private boolean allowInterval(String userId) {
        String key = "rag:qps:" + userId;
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofMillis(qpsIntervalMs));
        if (ok != null && ok) {
            return true;
        }
        log.warn("RAG qps limit exceeded: userId={}, intervalMs={}", userId, qpsIntervalMs);
        return false;
    }

    private boolean allowDaily(String userId) {
        String key = buildKey(userId);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, ttlToNextMidnight());
        }
        if (count != null && count > dailyLimit) {
            log.warn("RAG daily limit exceeded: userId={}, count={}, limit={}", userId, count, dailyLimit);
            return false;
        }
        return true;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    private static String normalize(String userId) {
        if (userId == null || userId.isBlank()) {
            return "anonymous";
        }
        return userId.trim().replaceAll("\\s+", "");
    }

    private static String buildKey(String userId) {
        String date = LocalDate.now(ZoneId.systemDefault()).toString();
        return "rag:daily:" + date + ":" + userId;
    }

    private static Duration ttlToNextMidnight() {
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime next = now.plusDays(1).toLocalDate().atStartOfDay(zone);
        return Duration.between(now, next);
    }
}
