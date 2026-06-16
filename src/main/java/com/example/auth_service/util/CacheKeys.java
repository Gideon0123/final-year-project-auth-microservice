package com.example.auth_service.util;

public class CacheKeys {

    public static final String USER = "users";
    public static final String USER_ALL = "'all'";

    public static String userById(Long id) {
        return String.valueOf(id);
    }
}