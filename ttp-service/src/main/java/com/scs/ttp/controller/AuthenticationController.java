package com.scs.ttp.controller;

import com.scs.dto.auth.ServerAuthenticationRequest;
import com.scs.dto.auth.TtpAuthenticationDecision;
import com.scs.ttp.service.AuthenticationService;
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
@RequestMapping("/api/ttp/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping({"/user", "/server", "/validate"})
    public TtpAuthenticationDecision authenticateUserForServer(@Valid @RequestBody ServerAuthenticationRequest request) {
        log.info("Handling TTP authentication request userId={} serverId={}", request.getUserId(), request.getServerId());
        return authenticationService.authenticateUserForServer(request);
    }
}
