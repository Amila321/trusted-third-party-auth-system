package com.scs.server.controller;

import com.scs.dto.auth.IdentityRegistrationRequest;
import com.scs.dto.auth.IdentityStatusResponse;
import com.scs.server.service.ServerRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/server/auth")
public class ServerRegistrationController {

    private final ServerRegistrationService serverRegistrationService;

    @PostMapping("/register")
    public IdentityStatusResponse register(@Valid @RequestBody IdentityRegistrationRequest request) {
        return serverRegistrationService.register(request);
    }

    @GetMapping("/identity")
    public IdentityStatusResponse identity() {
        return serverRegistrationService.currentIdentity();
    }
}
