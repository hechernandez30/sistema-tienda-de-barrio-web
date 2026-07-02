package com.tiendadebarrio.purchases.controller;

import com.tiendadebarrio.purchases.dto.PurchaseCreateRequest;
import com.tiendadebarrio.purchases.dto.PurchaseListResponse;
import com.tiendadebarrio.purchases.dto.PurchaseResponse;
import com.tiendadebarrio.purchases.service.PurchaseService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO')")
public class PurchaseController {

    private final PurchaseService purchaseService;

    @GetMapping
    public ResponseEntity<List<PurchaseListResponse>> list() {
        return ResponseEntity.ok(purchaseService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseService.getById(id));
    }

    @PostMapping
    public ResponseEntity<PurchaseResponse> create(@Valid @RequestBody PurchaseCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseService.create(request));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<PurchaseResponse> confirm(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseService.confirm(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PurchaseResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseService.cancel(id));
    }
}
