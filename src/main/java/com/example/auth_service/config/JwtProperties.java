package com.example.auth_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;

    private Access access;
    private Refresh refresh;

    @Getter
    @Setter
    public static class Access {
        private long expiration;
    }

    @Getter
    @Setter
    public static class Refresh {
        private long expiration;
    }
}