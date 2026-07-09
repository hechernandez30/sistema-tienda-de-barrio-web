package com.tiendadebarrio.inventory.service;

import com.tiendadebarrio.common.exception.ApiException;
import com.tiendadebarrio.inventory.dto.ProductLotResponse;
import com.tiendadebarrio.inventory.entity.ProductLot;
import com.tiendadebarrio.inventory.entity.SaleLotAllocation;
import com.tiendadebarrio.inventory.repository.ProductLotRepository;
import com.tiendadebarrio.inventory.repository.SaleLotAllocationRepository;
import com.tiendadebarrio.products.entity.Product;
import com.tiendadebarrio.products.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductLotService {

    private final ProductLotRepository productLotRepository;
    private final SaleLotAllocationRepository saleLotAllocationRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public BigDecimal getSellableQuantity(Product product) {
        if (!product.isTracksExpiration()) {
            return product.getCurrentStock();
        }
        return productLotRepository.sumSellableQuantity(product.getId(), LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<ProductLotResponse> listByProduct(UUID productId) {
        return productLotRepository.findByProductIdAndDeletedFalseOrderByExpirationDateAsc(productId)
                .stream()
                .filter(lot -> lot.getQuantity().signum() > 0)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductLotResponse> expiringWithinDays(int days) {
        LocalDate today = LocalDate.now();
        return productLotRepository.findExpiringBetween(today, today.plusDays(days))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductLotResponse> expiredWithStock() {
        return productLotRepository.findExpiredWithStock(LocalDate.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Ingresa stock en un lote nuevo (compra, ajuste de entrada o inventario inicial).
     */
    @Transactional
    public ProductLot receiveStock(
            Product product,
            BigDecimal quantity,
            LocalDate expirationDate,
            String lotCode,
            BigDecimal unitCost,
            UUID purchaseId,
            UUID purchaseItemId,
            String notes) {
        validateTracksExpiration(product);
        validateExpirationDate(expirationDate);
        validatePositiveQuantity(quantity);

        ProductLot lot = ProductLot.builder()
                .product(product)
                .lotCode(normalizeLotCode(lotCode))
                .expirationDate(expirationDate)
                .quantity(quantity)
                .unitCost(unitCost)
                .receivedAt(LocalDateTime.now())
                .purchaseId(purchaseId)
                .purchaseItemId(purchaseItemId)
                .notes(notes)
                .build();
        lot.setDeleted(false);

        ProductLot saved = productLotRepository.save(lot);
        syncProductStockFromLots(product);
        return saved;
    }

    /**
     * Descuenta stock aplicando FEFO (solo lotes no vencidos). Registra asignaciones por venta.
     */
    @Transactional
    public List<SaleLotAllocation> consumeFefoForSale(
            Product product,
            BigDecimal quantity,
            UUID saleId,
            UUID saleItemId) {
        validateTracksExpiration(product);
        validatePositiveQuantity(quantity);

        BigDecimal sellable = getSellableQuantity(product);
        if (sellable.compareTo(quantity) < 0) {
            throw new ApiException(
                    "Stock vendible insuficiente para " + product.getName()
                            + ". Disponible (no vencido): " + sellable,
                    HttpStatus.BAD_REQUEST,
                    "INSUFFICIENT_SELLABLE_STOCK"
            );
        }

        List<ProductLot> lots = productLotRepository.findSellableLotsFefo(product.getId(), LocalDate.now());
        BigDecimal remaining = quantity;
        List<SaleLotAllocation> allocations = new ArrayList<>();

        for (ProductLot lot : lots) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal take = lot.getQuantity().min(remaining);
            lot.setQuantity(lot.getQuantity().subtract(take));
            productLotRepository.save(lot);

            allocations.add(saleLotAllocationRepository.save(SaleLotAllocation.builder()
                    .saleId(saleId)
                    .saleItemId(saleItemId)
                    .productLotId(lot.getId())
                    .quantity(take)
                    .build()));

            remaining = remaining.subtract(take);
        }

        if (remaining.signum() > 0) {
            throw new ApiException(
                    "No fue posible asignar lotes suficientes para la venta",
                    HttpStatus.CONFLICT,
                    "LOT_ALLOCATION_FAILED"
            );
        }

        syncProductStockFromLots(product);
        return allocations;
    }

    /**
     * Descuenta stock por ajuste de salida (FEFO, incluye lotes vencidos si no hay otro).
     */
    @Transactional
    public List<UUID> consumeFefoForAdjustmentOut(Product product, BigDecimal quantity) {
        validateTracksExpiration(product);
        validatePositiveQuantity(quantity);

        if (product.getCurrentStock().compareTo(quantity) < 0) {
            throw new ApiException(
                    "Stock insuficiente para el ajuste de salida",
                    HttpStatus.BAD_REQUEST,
                    "INSUFFICIENT_STOCK"
            );
        }

        List<ProductLot> lots = productLotRepository
                .findByProductIdAndDeletedFalseOrderByExpirationDateAsc(product.getId())
                .stream()
                .filter(l -> l.getQuantity().signum() > 0)
                .toList();

        BigDecimal remaining = quantity;
        List<UUID> affectedLotIds = new ArrayList<>();

        for (ProductLot lot : lots) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal take = lot.getQuantity().min(remaining);
            lot.setQuantity(lot.getQuantity().subtract(take));
            productLotRepository.save(lot);
            affectedLotIds.add(lot.getId());
            remaining = remaining.subtract(take);
        }

        syncProductStockFromLots(product);
        return affectedLotIds;
    }

    /**
     * Restaura las asignaciones de una venta anulada (devuelve cantidad a cada lote).
     */
    @Transactional
    public void restoreSaleAllocations(UUID saleId) {
        List<SaleLotAllocation> allocations = saleLotAllocationRepository.findBySaleId(saleId);
        Set<UUID> productIds = new HashSet<>();

        for (SaleLotAllocation allocation : allocations) {
            ProductLot lot = productLotRepository.findById(allocation.getProductLotId())
                    .orElseThrow(() -> new ApiException(
                            "Lote no encontrado",
                            HttpStatus.CONFLICT,
                            "LOT_NOT_FOUND"
                    ));
            lot.setQuantity(lot.getQuantity().add(allocation.getQuantity()));
            productLotRepository.save(lot);
            productIds.add(lot.getProduct().getId());
        }

        for (UUID productId : productIds) {
            productRepository.findByIdAndDeletedFalse(productId)
                    .ifPresent(this::syncProductStockFromLots);
        }
    }

    @Transactional
    public void restoreSaleItemAllocations(UUID saleItemId) {
        List<SaleLotAllocation> allocations = saleLotAllocationRepository.findBySaleItemId(saleItemId);
        if (allocations.isEmpty()) {
            return;
        }

        UUID productId = null;
        for (SaleLotAllocation allocation : allocations) {
            ProductLot lot = productLotRepository.findById(allocation.getProductLotId())
                    .orElseThrow(() -> new ApiException(
                            "Lote no encontrado",
                            HttpStatus.CONFLICT,
                            "LOT_NOT_FOUND"
                    ));
            lot.setQuantity(lot.getQuantity().add(allocation.getQuantity()));
            productLotRepository.save(lot);
            productId = lot.getProduct().getId();
        }

        if (productId != null) {
            productRepository.findByIdAndDeletedFalse(productId)
                    .ifPresent(this::syncProductStockFromLots);
        }
    }

    /**
     * Revierte el lote creado por una línea de compra confirmada.
     */
    @Transactional
    public void reversePurchaseLot(UUID purchaseItemId, BigDecimal expectedQuantity) {
        ProductLot lot = productLotRepository.findByPurchaseItemIdAndDeletedFalse(purchaseItemId)
                .orElseThrow(() -> new ApiException(
                        "No se encontró el lote asociado a la compra",
                        HttpStatus.CONFLICT,
                        "PURCHASE_LOT_NOT_FOUND"
                ));

        if (lot.getQuantity().compareTo(expectedQuantity) < 0) {
            throw new ApiException(
                    "No se puede cancelar la compra: parte del lote ya fue vendida o ajustada",
                    HttpStatus.CONFLICT,
                    "PURCHASE_LOT_PARTIALLY_CONSUMED"
            );
        }

        Product product = lot.getProduct();
        lot.setQuantity(lot.getQuantity().subtract(expectedQuantity));
        if (lot.getQuantity().signum() == 0) {
            lot.setDeleted(true);
            lot.setDeletedAt(LocalDateTime.now());
        }
        productLotRepository.save(lot);
        syncProductStockFromLots(product);
    }

    @Transactional
    public void syncProductStockFromLots(Product product) {
        if (!product.isTracksExpiration()) {
            return;
        }
        BigDecimal total = productLotRepository.sumQuantityByProduct(product.getId());
        product.setCurrentStock(total);
        productRepository.save(product);
    }

    public void validateExpirationRequired(Product product, LocalDate expirationDate) {
        if (product.isTracksExpiration() && expirationDate == null) {
            throw new ApiException(
                    "La fecha de vencimiento es obligatoria para el producto " + product.getName(),
                    HttpStatus.BAD_REQUEST,
                    "EXPIRATION_DATE_REQUIRED"
            );
        }
    }

    public void validateExpirationNotInPast(LocalDate expirationDate) {
        if (expirationDate != null && expirationDate.isBefore(LocalDate.now())) {
            throw new ApiException(
                    "No se puede ingresar stock con fecha de vencimiento pasada",
                    HttpStatus.BAD_REQUEST,
                    "EXPIRATION_DATE_IN_PAST"
            );
        }
    }

    private void validateTracksExpiration(Product product) {
        if (!product.isTracksExpiration()) {
            throw new ApiException(
                    "El producto no controla vencimiento",
                    HttpStatus.BAD_REQUEST,
                    "PRODUCT_DOES_NOT_TRACK_EXPIRATION"
            );
        }
    }

    private void validateExpirationDate(LocalDate expirationDate) {
        if (expirationDate == null) {
            throw new ApiException(
                    "La fecha de vencimiento es obligatoria",
                    HttpStatus.BAD_REQUEST,
                    "EXPIRATION_DATE_REQUIRED"
            );
        }
        validateExpirationNotInPast(expirationDate);
    }

    private void validatePositiveQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new ApiException(
                    "La cantidad debe ser mayor que cero",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_QUANTITY"
            );
        }
    }

    private String normalizeLotCode(String lotCode) {
        return StringUtils.hasText(lotCode) ? lotCode.trim() : null;
    }

    private ProductLotResponse toResponse(ProductLot lot) {
        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(today, lot.getExpirationDate());
        return ProductLotResponse.builder()
                .id(lot.getId())
                .productId(lot.getProduct().getId())
                .productName(lot.getProduct().getName())
                .barcode(lot.getProduct().getBarcode())
                .lotCode(lot.getLotCode())
                .expirationDate(lot.getExpirationDate())
                .daysToExpire(days)
                .quantity(lot.getQuantity())
                .unitCost(lot.getUnitCost())
                .expired(lot.getExpirationDate().isBefore(today))
                .build();
    }
}
