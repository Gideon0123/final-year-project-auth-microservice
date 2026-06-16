package com.example.auth_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    public void saveCode(String key, String code, long minutes) {
        redisTemplate.opsForValue().set(key, code, Duration.ofMinutes(minutes));
    }

    public String getCode(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteCode(String key) {
        redisTemplate.delete(key);
    }

    public void blacklistToken(String token, long expiryMillis) {
        redisTemplate.opsForValue().set(
                "blacklist:" + token,
                "true",
                Duration.ofMillis(expiryMillis)
        );
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token));
    }
}