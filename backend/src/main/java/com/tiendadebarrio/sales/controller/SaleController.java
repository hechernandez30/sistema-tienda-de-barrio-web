package com.tiendadebarrio.sales.controller;

import com.tiendadebarrio.sales.dto.SaleCreateRequest;
import com.tiendadebarrio.sales.dto.SaleListResponse;
import com.tiendadebarrio.sales.dto.SaleResponse;
import com.tiendadebarrio.sales.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'REPORTES')")
    public ResponseEntity<List<SaleListResponse>> list() {
        return ResponseEntity.ok(saleService.list());
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'REPORTES')")
    public ResponseEntity<List<SaleListResponse>> today() {
        return ResponseEntity.ok(saleService.today());
    }

    @GetMapping("/by-date")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'REPORTES')")
    public ResponseEntity<List<SaleListResponse>> byDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(saleService.byDate(date));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'REPORTES')")
    public ResponseEntity<SaleResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(saleService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO')")
    public ResponseEntity<SaleResponse> create(@Valid @RequestBody SaleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(saleService.create(request));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SaleResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(saleService.cancel(id));
    }
}
