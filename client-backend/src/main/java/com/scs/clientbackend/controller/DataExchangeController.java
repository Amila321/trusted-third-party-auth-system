package com.scs.clientbackend.controller;

import com.scs.clientbackend.dto.DataExchangeResultResponse;
import com.scs.clientbackend.dto.PlaintextDataRequest;
import com.scs.clientbackend.dto.SessionCloseRequest;
import com.scs.clientbackend.dto.SessionCloseResponse;
import com.scs.clientbackend.service.DataExchangeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/client")
public class DataExchangeController {

    private final DataExchangeService dataExchangeService;

    @PostMapping("/data/encrypt-and-send")
    public DataExchangeResultResponse encryptAndSend(@Valid @RequestBody PlaintextDataRequest request) {
        return dataExchangeService.encryptAndSend(request);
    }

    @PostMapping("/session/close")
    public SessionCloseResponse closeSession(@Valid @RequestBody SessionCloseRequest request) {
        return dataExchangeService.closeSession(request.getSessionId());
    }
}
