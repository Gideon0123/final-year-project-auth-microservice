package com.example.auth_service.filter;

import com.example.auth_service.dto.RateLimitResponseDTO;
import com.example.auth_service.service.RateLimitService;
import com.example.auth_service.util.SecurityResponseUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final SecurityResponseUtil responseUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String key = generateKey(request);

        RateLimitResponseDTO rate = checkLimitByEndpoint(request, key);

        response.setHeader("X-Rate-Limit-Limit", String.valueOf(rate.getLimit()));
        response.setHeader("X-Rate-Limit-Remaining", String.valueOf(rate.getRemaining()));
        response.setHeader("X-Rate-Limit-Reset", String.valueOf(rate.getResetTime()));

        if (rate.isAllowed()) {
            filterChain.doFilter(request, response);
        } else {

            responseUtil.writeError(
                    request,
                    response,
                    429,
                    "Too many requests. Try again later."
            );
        }
    }

    private RateLimitResponseDTO checkLimitByEndpoint(HttpServletRequest request, String key) {

        String uri = request.getRequestURI();

        return switch (uri) {
            case "/auth/login" -> rateLimitService.checkRateLimit(key, 5, Duration.ofMinutes(1));
            case "/auth/register" -> rateLimitService.checkRateLimit(key, 50, Duration.ofMinutes(1));
            case "/auth/refresh-token" -> rateLimitService.checkRateLimit(key, 30, Duration.ofMinutes(1));
            default -> rateLimitService.checkRateLimit(key, 20, Duration.ofMinutes(1));
        };

    }

    private String generateKey(HttpServletRequest request) {

        String endpoint = request.getRequestURI();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)) {

            return "USER:" + auth.getName() + ":" + endpoint;
        }

        return "IP:" + request.getRemoteAddr() + ":" + endpoint;
    }
}