package com.scs.clientbackend.dto;

public record SystemStatusResponse(
        ServiceHealthResponse clientBackend,
        ServiceHealthResponse serverService,
        ServiceHealthResponse ttpService
) {
}