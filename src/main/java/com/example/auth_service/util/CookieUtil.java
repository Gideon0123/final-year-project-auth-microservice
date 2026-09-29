package com.example.auth_service.util;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

public final class CookieUtil {

    private CookieUtil() {
    }

    public static void addAccessToken(
            HttpServletResponse response,
            String token
    ) {

        ResponseCookie cookie =
                ResponseCookie.from("accessToken", token)
                        .httpOnly(true)
                        .secure(false) // true in production with HTTPS
                        .path("/")
                        .maxAge(Duration.ofMinutes(15))
                        .sameSite("Lax")
                        .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }


    public static void addRefreshToken(
            HttpServletResponse response,
            String token
    ) {

        ResponseCookie cookie =
                ResponseCookie.from("refreshToken", token)
                        .httpOnly(true)
                        .secure(false) // true in production with HTTPS
                        .path("/auth/refresh-token")
                        .maxAge(Duration.ofDays(7))
                        .sameSite("Lax")
                        .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }


    public static void clearCookies(
            HttpServletResponse response
    ) {

        ResponseCookie accessCookie =
                ResponseCookie.from("accessToken", "")
                        .httpOnly(true)
                        .secure(false)
                        .path("/")
                        .maxAge(Duration.ZERO)
                        .sameSite("Lax")
                        .build();

        ResponseCookie refreshCookie =
                ResponseCookie.from("refreshToken", "")
                        .httpOnly(true)
                        .secure(false)
                        .path("/auth/refresh-token")
                        .maxAge(Duration.ZERO)
                        .sameSite("Lax")
                        .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                accessCookie.toString()
        );

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                refreshCookie.toString()
        );
    }
}