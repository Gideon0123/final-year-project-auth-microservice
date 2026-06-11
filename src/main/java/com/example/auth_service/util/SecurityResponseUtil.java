package com.example.auth_service.util;

import com.example.auth_service.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class SecurityResponseUtil {

    private final ObjectMapper objectMapper = new ObjectMapper();

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

        response.setStatus(status);
        response.setContentType("application/json");

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
