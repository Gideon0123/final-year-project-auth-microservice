package com.example.auth_service.security;

import com.example.auth_service.dto.ApiResponse;
import com.example.auth_service.util.SecurityResponseUtil;
import com.example.auth_service.util.TraceIdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final SecurityResponseUtil responseUtil;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        ApiResponse<Object> apiResponse =
                ApiResponse.builder()
                        .success(false)
                        .message("Unauthorized")
                        .status(401)
                        .data(HttpStatus.UNAUTHORIZED.value())
                        .errors(List.of(authException.getMessage()))
                        .path(request.getRequestURI())
                        .traceId(TraceIdUtil.generate())
                        .timestamp(LocalDateTime.now())
                        .build();

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        objectMapper.writeValue(
                response.getOutputStream(),
                apiResponse
        );

        responseUtil.writeError(
                request,
                response,
                401,
                "Unauthorized!"
        );
    }
}