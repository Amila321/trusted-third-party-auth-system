package com.scs.clientbackend.controller;

import com.scs.clientbackend.service.AttackSimulationService;
import com.scs.dto.attack.AttackSimulationRequest;
import com.scs.dto.attack.AttackSimulationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/client/attack")
public class AttackSimulationController {

    private final AttackSimulationService attackSimulationService;

    @PostMapping("/simulate")
    public AttackSimulationResponse simulate(@Valid @RequestBody AttackSimulationRequest request) {
        return attackSimulationService.simulate(request);
    }
}
