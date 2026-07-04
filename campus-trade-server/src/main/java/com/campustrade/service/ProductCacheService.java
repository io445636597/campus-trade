package com.campustrade.service;

import com.campustrade.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ProductCacheService {

    private static final String DETAIL_PREFIX = "product:detail:";
    private static final String VIEW_RANK_KEY = "product:views:rank";
    private static final long DETAIL_TTL_MINUTES = 30;

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public ProductCacheService(RedisTemplate<String, Object> redisTemplate,
                               StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Get hot product IDs by view rank (ZREVRANGE).
     */
    public List<String> getHotProducts(int limit) {
        try {
            Set<String> ids = stringRedisTemplate.opsForZSet()
                    .reverseRange(VIEW_RANK_KEY, 0, limit - 1);
            return ids != null ? new ArrayList<>(ids) : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Redis error in getHotProducts: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Cache product detail with JSON serialization, TTL 30 minutes.
     */
    public void cacheProductDetail(Product product) {
        try {
            String key = DETAIL_PREFIX + product.getId();
            redisTemplate.opsForValue().set(key, product, DETAIL_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis error in cacheProductDetail({}): {}", product.getId(), e.getMessage());
        }
    }

    /**
     * Get product detail from cache.
     */
    public Product getProductDetail(Long id) {
        try {
            String key = DETAIL_PREFIX + id;
            Object obj = redisTemplate.opsForValue().get(key);
            if (obj instanceof Product) {
                return (Product) obj;
            }
            return null;
        } catch (Exception e) {
            log.warn("Redis error in getProductDetail({}): {}", id, e.getMessage());
            return null;
        }
    }

    /**
     * Evict product detail from cache.
     */
    public void evictProduct(Long id) {
        try {
            String key = DETAIL_PREFIX + id;
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis error in evictProduct({}): {}", id, e.getMessage());
        }
    }

    /**
     * Increment view rank in sorted set.
     */
    public void incrementViewRank(Long productId) {
        try {
            stringRedisTemplate.opsForZSet()
                    .incrementScore(VIEW_RANK_KEY, String.valueOf(productId), 1);
        } catch (Exception e) {
            log.warn("Redis error in incrementViewRank({}): {}", productId, e.getMessage());
        }
    }
}
