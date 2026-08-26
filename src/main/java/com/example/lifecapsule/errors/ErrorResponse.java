package com.example.lifecapsule.errors;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fields
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, null);
    }

    public static ErrorResponse withFields(
            int status,
            String error,
            String message,
            String path,
            Map<String, String> fields
    ) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, fields);
    }
}
