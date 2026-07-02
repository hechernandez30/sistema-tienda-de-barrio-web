package com.tiendadebarrio.products.controller;

import com.tiendadebarrio.products.dto.ProductCreateRequest;
import com.tiendadebarrio.products.dto.ProductDetailResponse;
import com.tiendadebarrio.products.dto.ProductListResponse;
import com.tiendadebarrio.products.dto.ProductPosResponse;
import com.tiendadebarrio.products.dto.ProductUpdateRequest;
import com.tiendadebarrio.products.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO')")
    public ResponseEntity<List<ProductListResponse>> list() {
        return ResponseEntity.ok(productService.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO')")
    public ResponseEntity<ProductDetailResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @GetMapping("/barcode/{barcode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO', 'CAJERO')")
    public ResponseEntity<ProductPosResponse> getByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(productService.getByBarcode(barcode));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO', 'CAJERO')")
    public ResponseEntity<List<ProductPosResponse>> search(@RequestParam String term) {
        return ResponseEntity.ok(productService.search(term));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO')")
    public ResponseEntity<ProductDetailResponse> create(@Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO')")
    public ResponseEntity<ProductDetailResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
