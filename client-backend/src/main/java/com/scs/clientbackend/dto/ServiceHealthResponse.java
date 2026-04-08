package com.scs.clientbackend.dto;

public record ServiceHealthResponse(
        String service,
        String status
) {
}