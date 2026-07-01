package com.example.auth_service.util;

import com.example.auth_service.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SecurityResponseUtil {

    private final ObjectMapper objectMapper;

    public void writeError(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {

        ApiResponse<Object> apiResponse = ApiResponse.builder()
                .success(false)
                .message(message)
                .status(status)
                .data(null)
                .errors(List.of(message))
                .path(request.getRequestURI())
                .traceId(TraceIdUtil.generate())
                .timestamp(LocalDateTime.now())
                .build();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
