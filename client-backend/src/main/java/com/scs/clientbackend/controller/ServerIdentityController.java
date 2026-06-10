package com.scs.clientbackend.controller;

import com.scs.clientbackend.service.ServerConnection;
import com.scs.dto.auth.IdentityRegistrationRequest;
import com.scs.dto.auth.IdentityStatusResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/client/server")
public class ServerIdentityController {

    private final ServerConnection serverConnection;

    @PostMapping("/register")
    public IdentityStatusResponse registerServer(@Valid @RequestBody IdentityRegistrationRequest request) {
        return serverConnection.registerServer(request);
    }

    @GetMapping("/identity")
    public IdentityStatusResponse serverIdentity() {
        return serverConnection.getServerIdentity();
    }
}
