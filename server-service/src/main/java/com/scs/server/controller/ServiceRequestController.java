package com.scs.server.controller;

import com.scs.dto.auth.TtpAuthenticationDecision;
import com.scs.dto.auth.UserAuthenticationRequest;
import com.scs.server.service.ServerAuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/server")
public class ServiceRequestController {

    private final ServerAuthenticationService serverAuthenticationService;

    @PostMapping("/request")
    public TtpAuthenticationDecision requestService(@Valid @RequestBody UserAuthenticationRequest request) {
        log.info("Received user service request userId={} serverId={}", request.getUserId(), request.getServerId());
        return serverAuthenticationService.handleUserServiceRequest(request);
    }
}
