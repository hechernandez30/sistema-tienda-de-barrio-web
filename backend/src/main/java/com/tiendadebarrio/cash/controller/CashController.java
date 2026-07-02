package com.tiendadebarrio.cash.controller;

import com.tiendadebarrio.cash.dto.CashMovementRequest;
import com.tiendadebarrio.cash.dto.CashMovementResponse;
import com.tiendadebarrio.cash.dto.CashSessionResponse;
import com.tiendadebarrio.cash.dto.CashSummaryResponse;
import com.tiendadebarrio.cash.dto.CloseCashSessionRequest;
import com.tiendadebarrio.cash.dto.OpenCashSessionRequest;
import com.tiendadebarrio.cash.service.CashService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cash")
@RequiredArgsConstructor
public class CashController {

    private final CashService cashService;

    @PostMapping("/sessions/open")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO')")
    public ResponseEntity<CashSessionResponse> openSession(@Valid @RequestBody OpenCashSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cashService.openSession(request));
    }

    @PostMapping("/sessions/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO')")
    public ResponseEntity<CashSessionResponse> closeSession(
            @PathVariable UUID id,
            @Valid @RequestBody CloseCashSessionRequest request) {
        return ResponseEntity.ok(cashService.closeSession(id, request));
    }

    @GetMapping("/sessions/current")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'REPORTES')")
    public ResponseEntity<CashSummaryResponse> current() {
        return ResponseEntity.ok(cashService.getCurrentSummary());
    }

    @GetMapping("/sessions")
    @PreAuthorize("hasAnyRole('ADMIN', 'REPORTES')")
    public ResponseEntity<List<CashSessionResponse>> listSessions() {
        return ResponseEntity.ok(cashService.listSessions());
    }

    @GetMapping("/sessions/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REPORTES')")
    public ResponseEntity<CashSessionResponse> getSession(@PathVariable UUID id) {
        return ResponseEntity.ok(cashService.getSession(id));
    }

    @GetMapping("/sessions/{id}/movements")
    @PreAuthorize("hasAnyRole('ADMIN', 'REPORTES')")
    public ResponseEntity<List<CashMovementResponse>> sessionMovements(@PathVariable UUID id) {
        return ResponseEntity.ok(cashService.getSessionMovements(id));
    }

    @PostMapping("/movements")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO')")
    public ResponseEntity<CashMovementResponse> registerMovement(@Valid @RequestBody CashMovementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cashService.registerManualMovement(request));
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('ADMIN', 'REPORTES')")
    public ResponseEntity<List<CashMovementResponse>> listMovements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(cashService.listMovements(page, size));
    }
}
