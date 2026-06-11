package com.example.auth_service.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

public class CookieUtil {

    public static void addAccessToken(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("accessToken", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // true in production (HTTPS)
        cookie.setPath("/");
        cookie.setMaxAge(60 * 15);

        response.addCookie(cookie);
    }

    public static void addRefreshToken(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refreshToken", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("auth/refresh-token"); // scoped
        cookie.setMaxAge(60 * 60 * 24 * 7); // 7 days

        response.addCookie(cookie);
    }

    public static void clearCookies(HttpServletResponse response) {
        Cookie access = new Cookie("accessToken", null);
        access.setHttpOnly(true);
        access.setPath("/");
        access.setMaxAge(0);

        Cookie refresh = new Cookie("refreshToken", null);
        refresh.setHttpOnly(true);
        refresh.setPath("/api/v1/auth/refresh");
        refresh.setMaxAge(0);

        response.addCookie(access);
        response.addCookie(refresh);
    }
}