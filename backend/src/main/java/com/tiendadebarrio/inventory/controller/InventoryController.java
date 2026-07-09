package com.tiendadebarrio.inventory.controller;

import com.tiendadebarrio.inventory.dto.InventoryAdjustmentRequest;
import com.tiendadebarrio.inventory.dto.InventoryMovementResponse;
import com.tiendadebarrio.inventory.dto.LowStockResponse;
import com.tiendadebarrio.inventory.dto.ProductStockResponse;
import com.tiendadebarrio.inventory.dto.ProductLotResponse;
import com.tiendadebarrio.inventory.service.InventoryService;
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
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO')")
    public ResponseEntity<List<InventoryMovementResponse>> listMovements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(inventoryService.listMovements(page, size));
    }

    @GetMapping("/products/{productId}/movements")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO')")
    public ResponseEntity<List<InventoryMovementResponse>> listProductMovements(@PathVariable UUID productId) {
        return ResponseEntity.ok(inventoryService.listProductMovements(productId));
    }

    @GetMapping("/products/{productId}/stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO')")
    public ResponseEntity<ProductStockResponse> getStock(@PathVariable UUID productId) {
        return ResponseEntity.ok(inventoryService.getStock(productId));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO', 'REPORTES')")
    public ResponseEntity<List<LowStockResponse>> lowStock() {
        return ResponseEntity.ok(inventoryService.lowStock());
    }

    @GetMapping("/products/{productId}/lots")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO')")
    public ResponseEntity<List<ProductLotResponse>> listProductLots(@PathVariable UUID productId) {
        return ResponseEntity.ok(inventoryService.listProductLots(productId));
    }

    @GetMapping("/lots/expiring")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO', 'REPORTES')")
    public ResponseEntity<List<ProductLotResponse>> expiringLots(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(inventoryService.expiringLots(days));
    }

    @GetMapping("/lots/expired")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO', 'REPORTES')")
    public ResponseEntity<List<ProductLotResponse>> expiredLots() {
        return ResponseEntity.ok(inventoryService.expiredLots());
    }

    @PostMapping("/adjustments/in")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO')")
    public ResponseEntity<InventoryMovementResponse> adjustIn(
            @Valid @RequestBody InventoryAdjustmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.adjustIn(request));
    }

    @PostMapping("/adjustments/out")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO')")
    public ResponseEntity<InventoryMovementResponse> adjustOut(
            @Valid @RequestBody InventoryAdjustmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.adjustOut(request));
    }
}
