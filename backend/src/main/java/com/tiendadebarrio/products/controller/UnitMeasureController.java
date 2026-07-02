package com.tiendadebarrio.products.controller;

import com.tiendadebarrio.products.dto.UnitMeasureCreateRequest;
import com.tiendadebarrio.products.dto.UnitMeasureResponse;
import com.tiendadebarrio.products.service.UnitMeasureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/unit-measures")
@RequiredArgsConstructor
public class UnitMeasureController {

    private final UnitMeasureService unitMeasureService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO')")
    public ResponseEntity<List<UnitMeasureResponse>> list() {
        return ResponseEntity.ok(unitMeasureService.list());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO')")
    public ResponseEntity<UnitMeasureResponse> create(@Valid @RequestBody UnitMeasureCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(unitMeasureService.create(request));
    }
}
