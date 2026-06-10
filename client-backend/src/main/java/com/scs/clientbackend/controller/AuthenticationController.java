package com.scs.clientbackend.controller;

import com.scs.clientbackend.dto.ClientAuthenticationInitiateRequest;
import com.scs.clientbackend.dto.ClientAuthenticationStateResponse;
import com.scs.clientbackend.dto.ClientSessionStatusResponse;
import com.scs.clientbackend.service.ClientAuthenticationService;
import com.scs.dto.auth.TtpAuthenticationDecision;
import com.scs.dto.session.SessionKeyResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/client/auth")
public class AuthenticationController {

    private final ClientAuthenticationService clientAuthenticationService;

    @PostMapping("/initiate")
    public ClientAuthenticationStateResponse initiate(@Valid @RequestBody ClientAuthenticationInitiateRequest request) {
        return clientAuthenticationService.initiate(request);
    }

    @PostMapping("/complete")
    public ClientSessionStatusResponse complete(@Valid @RequestBody SessionKeyResponse response) {
        return clientAuthenticationService.complete(response);
    }

    @PostMapping("/request-session")
    public TtpAuthenticationDecision requestSession() {
        return clientAuthenticationService.requestSession();
    }

    @PostMapping("/request-and-complete-session")
    public ClientSessionStatusResponse requestAndCompleteSession() {
        return clientAuthenticationService.requestAndCompleteSession();
    }
}
