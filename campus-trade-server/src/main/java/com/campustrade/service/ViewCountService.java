package com.campustrade.service;

import com.campustrade.entity.Product;
import com.campustrade.mapper.ProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ViewCountService {

    private static final String VIEW_PREFIX = "product:view:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ProductMapper productMapper;

    public ViewCountService(StringRedisTemplate stringRedisTemplate,
                            ProductMapper productMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.productMapper = productMapper;
    }

    /**
     * Increment view count in Redis (INCR).
     */
    public void incrementView(Long productId) {
        try {
            String key = VIEW_PREFIX + productId;
            stringRedisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.warn("Redis error in incrementView({}): {}", productId, e.getMessage());
        }
    }

    /**
     * Get buffered view count from Redis.
     */
    public int getViewCount(Long productId) {
        try {
            String key = VIEW_PREFIX + productId;
            String value = stringRedisTemplate.opsForValue().get(key);
            if (value != null) {
                return Integer.parseInt(value);
            }
        } catch (Exception e) {
            log.warn("Redis error in getViewCount({}): {}", productId, e.getMessage());
        }
        return 0;
    }

    /**
     * Flush buffered view counts to database every 5 minutes.
     */
    @Scheduled(fixedRate = 300000)
    public void flushToDB() {
        List<String> keys = new ArrayList<>();
        try {
            ScanOptions scanOptions = ScanOptions.scanOptions()
                    .match(VIEW_PREFIX + "*")
                    .count(100)
                    .build();

            try (Cursor<String> cursor = stringRedisTemplate.scan(scanOptions)) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                }
            }

            int flushed = 0;
            for (String key : keys) {
                try {
                    String value = stringRedisTemplate.opsForValue().get(key);
                    if (value != null) {
                        int count = Integer.parseInt(value);
                        if (count > 0) {
                            // Extract product ID from key
                            String idStr = key.substring(VIEW_PREFIX.length());
                            Long productId = Long.parseLong(idStr);

                            Product product = productMapper.selectById(productId);
                            if (product != null) {
                                int current = product.getViewCount() == null ? 0 : product.getViewCount();
                                product.setViewCount(current + count);
                                productMapper.updateById(product);
                                flushed++;
                            }
                        }
                        // Delete the Redis key after flushing
                        stringRedisTemplate.delete(key);
                    }
                } catch (Exception e) {
                    log.warn("Failed to flush view count for key {}: {}", key, e.getMessage());
                }
            }

            if (flushed > 0) {
                log.info("View count flush completed: {} products flushed", flushed);
            }
        } catch (Exception e) {
            log.warn("Redis error during flushToDB scan: {}", e.getMessage());
        }
    }
}
