package com.scs.clientbackend.controller;

import com.scs.clientbackend.dto.SystemStatusResponse;
import com.scs.clientbackend.service.SystemStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemStatusController {

    private final SystemStatusService systemStatusService;

    public SystemStatusController(SystemStatusService systemStatusService) {
        this.systemStatusService = systemStatusService;
    }

    @GetMapping("/api/client/status")
    public SystemStatusResponse getSystemStatus() {
        return systemStatusService.getSystemStatus();
    }
}