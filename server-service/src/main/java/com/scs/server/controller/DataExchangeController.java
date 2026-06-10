package com.scs.server.controller;

import com.scs.dto.data.EncryptedDataRequest;
import com.scs.dto.data.EncryptedDataResponse;
import com.scs.server.dto.SessionCloseRequest;
import com.scs.server.dto.SessionCloseResponse;
import com.scs.server.service.DataExchangeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/server")
public class DataExchangeController {

    private final DataExchangeService dataExchangeService;

    @PostMapping("/data/decrypt-and-process")
    public EncryptedDataResponse decryptAndProcess(@Valid @RequestBody EncryptedDataRequest request) {
        return dataExchangeService.decryptAndProcess(request);
    }

    @PostMapping("/session/close")
    public SessionCloseResponse closeSession(@Valid @RequestBody SessionCloseRequest request) {
        return dataExchangeService.closeSession(request.getSessionId());
    }
}
