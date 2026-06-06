package com.example.auth_service.service;

import com.example.auth_service.config.JwtProperties;
import com.example.auth_service.entity.User;
import com.example.auth_service.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private final JwtProperties jwtProperties;
    private final RedisTemplate<String, Object> redisTemplate;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(
                jwtProperties.getSecret()
        );

        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

//    private SecretKey getSigningKey() {
//
//        byte[] keyBytes = Decoders.BASE64.decode(
//                jwtProperties.getSecret()
//        );
//
//        return Keys.hmacShaKeyFor(keyBytes);
//    }

    public String extractToken(
            HttpServletRequest request
    ) {
        String authHeader = request.getHeader(
                HttpHeaders.AUTHORIZATION
        );

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {

                    return cookie.getValue();
                }
            }
        }

        throw new InvalidTokenException("Access token not found");
    }

    public String generateAccessToken(
            User user
    ) {

        return Jwts.builder()
                .subject(user.getEmail())
                .claim(
                        "userId",
                        user.getId()
                )
                .claim(
                        "role",
                        user.getRole().name()
                )
                .claim(
                        "tokenType",
                        "ACCESS"
                )
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + jwtProperties.getAccessTokenExpiration()
                        )
                )
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(
            User user
    ) {

        return Jwts.builder()
                .subject(user.getEmail())
                .claim(
                        "userId",
                        user.getId()
                )
                .claim(
                        "tokenType",
                        "REFRESH"
                )
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + jwtProperties.getRefreshTokenExpiration()
                        )
                )
                .signWith(signingKey)
                .compact();
    }

    private Claims extractAllClaims(
            String token
    ) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public <T> T extractClaim(
            String token,
            Function<Claims, T> resolver
    ) {
        return resolver.apply(extractAllClaims(token));
    }

    public String extractUsername(
            String token
    ) {
        return extractClaim(
                token,
                Claims::getSubject
        );
    }

    public Long extractUserId(
            String token
    ) {
        return extractClaim(
                token,
                claims -> claims.get(
                        "userId",
                        Long.class
                )
        );
    }

    public String extractRole(
            String token
    ) {
        return extractClaim(
                token,
                claims -> claims.get(
                        "role",
                        String.class
                )
        );
    }

    public String extractTokenType(
            String token
    ) {
        return extractClaim(
                token,
                claims -> claims.get(
                        "tokenType",
                        String.class
                )
        );
    }

    public boolean isTokenExpired(
            String token
    ) {
        Date expiration = extractClaim(
                token,
                Claims::getExpiration
        );

        return expiration.before(
                new Date()
        );
    }

    public boolean validateToken(
            String token
    ) {
        try {
            return !isTokenExpired(token) && !isBlacklisted(token);
        }
        catch (Exception ex) {
            log.error(
                    "JWT validation failed",
                    ex
            );

            return false;
        }
    }

    public boolean validateAccessToken(
            String token
    ) {
        return validateToken(token) && "ACCESS".equals(extractTokenType(token));
    }

    public boolean validateRefreshToken(
            String token
    ) {
        return validateToken(token) && "REFRESH".equals(extractTokenType(token));
    }

    public void blacklistToken(
            String token
    ) {
        Date expiration = extractClaim(
                token,
                Claims::getExpiration
        );

        long ttl = expiration.getTime() - System.currentTimeMillis();

        redisTemplate.opsForValue().set(
                "blacklist:" + token,
                "BLACKLISTED",
                Duration.ofMillis(ttl)
        );
    }

    public boolean isBlacklisted(
            String token
    ) {
        Boolean exists = redisTemplate.hasKey(
                "blacklist:" + token
        );
        return Boolean.TRUE.equals(exists);
    }

    public ResponseCookie buildAccessCookie(
            String token
    ) {
        return ResponseCookie.from("accessToken", token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofMinutes(15))
                .sameSite("Lax")
                .build();
    }

    public ResponseCookie buildRefreshCookie(
            String token
    ) {
        return ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("Lax")
                .build();
    }
}