package com.campustrade.service;

import com.campustrade.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RateLimitService {

    private static final String RATE_PREFIX = "rate:login:";
    private static final int MAX_REQUESTS = 5;
    private static final int WINDOW_SECONDS = 60;

    private final StringRedisTemplate stringRedisTemplate;

    public RateLimitService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Check login rate limit by IP. Throws BusinessException(429) if exceeded.
     * Fails open: if Redis is down, allow the request through.
     */
    public void checkLoginLimit(String ip) {
        try {
            String key = RATE_PREFIX + ip;
            Long count = stringRedisTemplate.opsForValue().increment(key);

            // Set TTL on first request in the window
            if (count != null && count == 1) {
                stringRedisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
            }

            if (count != null && count > MAX_REQUESTS) {
                throw new BusinessException(429, "操作过于频繁，请1分钟后再试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis error in checkLoginLimit({}): {} — allowing through", ip, e.getMessage());
            // Fail-open: allow the request through if Redis is unavailable
        }
    }
}
