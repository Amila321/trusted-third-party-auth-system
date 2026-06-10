package com.scs.ttp.controller;

import com.scs.dto.auth.RegistrationRequest;
import com.scs.dto.auth.RegistrationResponse;
import com.scs.ttp.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ttp")
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/register/user")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse registerUser(@Valid @RequestBody RegistrationRequest request) {
        log.info("Handling user registration request for identityName={}", request.getIdentityName());
        return registrationService.registerUser(request);
    }

    @PostMapping("/register/server")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse registerServer(@Valid @RequestBody RegistrationRequest request) {
        log.info("Handling server registration request for identityName={}", request.getIdentityName());
        return registrationService.registerServer(request);
    }

    @GetMapping("/certificate/{identityId}")
    public RegistrationResponse getCertificate(@PathVariable("identityId") String identityId) {
        log.info("Handling certificate retrieval for identityId={}", identityId);
        return registrationService.getCertificate(identityId);
    }
}
