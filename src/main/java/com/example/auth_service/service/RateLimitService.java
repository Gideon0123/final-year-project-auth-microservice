package com.example.auth_service.service;

import com.example.auth_service.dto.RateLimitResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RateLimitResponseDTO checkRateLimit(String key, int limit, Duration duration) {

        String redisKey = "rate_limit:" + key;

        Long currentCount = redisTemplate.opsForValue().increment(redisKey);

        if (currentCount == 1) {
            redisTemplate.expire(redisKey, duration);
        }

        long remaining = Math.max(0, limit - currentCount);

        Long ttl = redisTemplate.getExpire(redisKey);

        return new RateLimitResponseDTO(
                currentCount <= limit,
                remaining,
                limit,
                ttl != null ? ttl : 0
        );
    }
}