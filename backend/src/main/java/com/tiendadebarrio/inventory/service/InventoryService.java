package com.tiendadebarrio.inventory.service;

import com.tiendadebarrio.audit.service.AuditService;
import com.tiendadebarrio.common.exception.ApiException;
import com.tiendadebarrio.inventory.dto.InventoryAdjustmentRequest;
import com.tiendadebarrio.inventory.dto.InventoryMovementResponse;
import com.tiendadebarrio.inventory.dto.LowStockResponse;
import com.tiendadebarrio.inventory.dto.ProductLotResponse;
import com.tiendadebarrio.inventory.dto.ProductStockResponse;
import com.tiendadebarrio.inventory.entity.InventoryMovement;
import com.tiendadebarrio.inventory.entity.InventoryMovementType;
import com.tiendadebarrio.inventory.entity.ProductLot;
import com.tiendadebarrio.inventory.mapper.InventoryMapper;
import com.tiendadebarrio.inventory.repository.InventoryMovementRepository;
import com.tiendadebarrio.products.dto.InitialLotRequest;
import com.tiendadebarrio.products.entity.Product;
import com.tiendadebarrio.products.repository.ProductRepository;
import com.tiendadebarrio.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final String AUDIT_MODULE = "INVENTORY";
    private static final String AUDIT_ENTITY = "InventoryMovement";
    private static final String INITIAL_STOCK_NOTE = "Inventario inicial";

    private final ProductRepository productRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final InventoryMapper inventoryMapper;
    private final AuditService auditService;
    private final ProductLotService productLotService;

    @Transactional(readOnly = true)
    public List<InventoryMovementResponse> listMovements(int page, int size) {
        return inventoryMovementRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(inventoryMapper::toMovementResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryMovementResponse> listProductMovements(UUID productId) {
        Product product = findActiveProduct(productId);
        return inventoryMovementRepository.findByProductIdOrderByCreatedAtDesc(product.getId())
                .stream()
                .map(inventoryMapper::toMovementResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductStockResponse getStock(UUID productId) {
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(this::productNotFound);
        return inventoryMapper.toStockResponse(product, productLotService.getSellableQuantity(product));
    }

    @Transactional(readOnly = true)
    public List<LowStockResponse> lowStock() {
        return productRepository.findLowStock()
                .stream()
                .map(product -> inventoryMapper.toLowStockResponse(
                        product,
                        productLotService.getSellableQuantity(product)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductLotResponse> listProductLots(UUID productId) {
        Product product = findActiveProduct(productId);
        if (!product.isTracksExpiration()) {
            throw new ApiException(
                    "El producto no controla vencimiento",
                    HttpStatus.BAD_REQUEST,
                    "PRODUCT_DOES_NOT_TRACK_EXPIRATION"
            );
        }
        return productLotService.listByProduct(product.getId());
    }

    @Transactional(readOnly = true)
    public List<ProductLotResponse> expiringLots(int days) {
        return productLotService.expiringWithinDays(days);
    }

    @Transactional(readOnly = true)
    public List<ProductLotResponse> expiredLots() {
        return productLotService.expiredWithStock();
    }

    @Transactional
    public InventoryMovementResponse adjustIn(InventoryAdjustmentRequest request) {
        Product product = findActiveProduct(request.getProductId());

        if (product.isTracksExpiration()) {
            productLotService.validateExpirationRequired(product, request.getExpirationDate());
            productLotService.validateExpirationNotInPast(request.getExpirationDate());

            BigDecimal previousStock = product.getCurrentStock();
            ProductLot lot = productLotService.receiveStock(
                    product,
                    request.getQuantity(),
                    request.getExpirationDate(),
                    request.getLotCode(),
                    request.getUnitCost(),
                    null,
                    null,
                    request.getNotes());

            Product refreshed = refreshProduct(product.getId());
            InventoryMovement saved = recordMovementAfterStockChange(
                    refreshed,
                    InventoryMovementType.ADJUSTMENT_IN,
                    request.getQuantity(),
                    previousStock,
                    refreshed.getCurrentStock(),
                    request.getUnitCost(),
                    null,
                    null,
                    lot,
                    request.getNotes());

            auditService.record(
                    InventoryMovementType.ADJUSTMENT_IN.name(),
                    AUDIT_MODULE,
                    AUDIT_ENTITY,
                    saved.getId(),
                    null,
                    auditSnapshot(saved));

            return inventoryMapper.toMovementResponse(saved);
        }

        return registerAdjustment(request, InventoryMovementType.ADJUSTMENT_IN);
    }

    @Transactional
    public InventoryMovementResponse adjustOut(InventoryAdjustmentRequest request) {
        Product product = findActiveProduct(request.getProductId());

        if (product.isTracksExpiration()) {
            BigDecimal previousStock = product.getCurrentStock();
            productLotService.consumeFefoForAdjustmentOut(product, request.getQuantity());

            Product refreshed = refreshProduct(product.getId());
            InventoryMovement saved = recordMovementAfterStockChange(
                    refreshed,
                    InventoryMovementType.ADJUSTMENT_OUT,
                    request.getQuantity(),
                    previousStock,
                    refreshed.getCurrentStock(),
                    request.getUnitCost(),
                    null,
                    null,
                    null,
                    request.getNotes());

            auditService.record(
                    InventoryMovementType.ADJUSTMENT_OUT.name(),
                    AUDIT_MODULE,
                    AUDIT_ENTITY,
                    saved.getId(),
                    null,
                    auditSnapshot(saved));

            return inventoryMapper.toMovementResponse(saved);
        }

        return registerAdjustment(request, InventoryMovementType.ADJUSTMENT_OUT);
    }

    private InventoryMovementResponse registerAdjustment(
            InventoryAdjustmentRequest request,
            InventoryMovementType type) {
        Product product = findActiveProduct(request.getProductId());

        InventoryMovement saved = registerMovement(
                product,
                type,
                request.getQuantity(),
                request.getUnitCost(),
                null,
                null,
                request.getNotes());

        auditService.record(
                type.name(),
                AUDIT_MODULE,
                AUDIT_ENTITY,
                saved.getId(),
                null,
                auditSnapshot(saved));

        return inventoryMapper.toMovementResponse(saved);
    }

    @Transactional
    public InventoryMovement registerInitialStock(Product product, BigDecimal quantity, BigDecimal unitCost) {
        if (product.isTracksExpiration()) {
            throw new ApiException(
                    "Use lotes iniciales para productos con control de vencimiento",
                    HttpStatus.BAD_REQUEST,
                    "USE_INITIAL_LOTS"
            );
        }

        InventoryMovement saved = registerMovement(
                product,
                InventoryMovementType.ADJUSTMENT_IN,
                quantity,
                unitCost,
                null,
                null,
                INITIAL_STOCK_NOTE);

        auditService.record(
                InventoryMovementType.ADJUSTMENT_IN.name(),
                AUDIT_MODULE,
                AUDIT_ENTITY,
                saved.getId(),
                null,
                auditSnapshot(saved));

        return saved;
    }

    @Transactional
    public void registerInitialLotStock(Product product, InitialLotRequest lotRequest, BigDecimal unitCost) {
        BigDecimal previousStock = product.getCurrentStock();
        ProductLot lot = productLotService.receiveStock(
                product,
                lotRequest.getQuantity(),
                lotRequest.getExpirationDate(),
                lotRequest.getLotCode(),
                unitCost,
                null,
                null,
                INITIAL_STOCK_NOTE);

        Product refreshed = refreshProduct(product.getId());
        InventoryMovement saved = recordMovementAfterStockChange(
                refreshed,
                InventoryMovementType.ADJUSTMENT_IN,
                lotRequest.getQuantity(),
                previousStock,
                refreshed.getCurrentStock(),
                unitCost,
                null,
                null,
                lot,
                INITIAL_STOCK_NOTE);

        auditService.record(
                InventoryMovementType.ADJUSTMENT_IN.name(),
                AUDIT_MODULE,
                AUDIT_ENTITY,
                saved.getId(),
                null,
                auditSnapshot(saved));
    }

    @Transactional
    public InventoryMovement registerSaleMovement(
            Product product,
            BigDecimal quantity,
            UUID saleId,
            UUID saleItemId,
            String notes) {
        if (product.isTracksExpiration()) {
            BigDecimal previousStock = product.getCurrentStock();
            productLotService.consumeFefoForSale(product, quantity, saleId, saleItemId);

            Product refreshed = refreshProduct(product.getId());
            return recordMovementAfterStockChange(
                    refreshed,
                    InventoryMovementType.SALE,
                    quantity,
                    previousStock,
                    refreshed.getCurrentStock(),
                    null,
                    null,
                    saleId,
                    null,
                    notes);
        }

        return registerMovement(product, InventoryMovementType.SALE, quantity, null, null, saleId, notes);
    }

    @Transactional
    public InventoryMovement registerSaleCancellationMovement(
            Product product,
            BigDecimal quantity,
            UUID saleId,
            String notes) {
        return registerMovement(product, InventoryMovementType.SALE_CANCEL, quantity, null, null, saleId, notes);
    }

    @Transactional
    public InventoryMovement registerPurchaseMovement(
            Product product,
            BigDecimal quantity,
            BigDecimal unitCost,
            UUID purchaseId,
            UUID purchaseItemId,
            LocalDate expirationDate,
            String lotCode,
            String notes) {
        if (product.isTracksExpiration()) {
            productLotService.validateExpirationRequired(product, expirationDate);
            productLotService.validateExpirationNotInPast(expirationDate);

            BigDecimal previousStock = product.getCurrentStock();
            ProductLot lot = productLotService.receiveStock(
                    product,
                    quantity,
                    expirationDate,
                    lotCode,
                    unitCost,
                    purchaseId,
                    purchaseItemId,
                    notes);

            Product refreshed = refreshProduct(product.getId());
            return recordMovementAfterStockChange(
                    refreshed,
                    InventoryMovementType.PURCHASE,
                    quantity,
                    previousStock,
                    refreshed.getCurrentStock(),
                    unitCost,
                    purchaseId,
                    null,
                    lot,
                    notes);
        }

        return registerMovement(
                product,
                InventoryMovementType.PURCHASE,
                quantity,
                unitCost,
                purchaseId,
                null,
                notes);
    }

    @Transactional
    public InventoryMovement registerPurchaseCancellationMovement(
            Product product,
            BigDecimal quantity,
            BigDecimal unitCost,
            UUID purchaseId,
            UUID purchaseItemId,
            String notes) {
        if (product.isTracksExpiration()) {
            BigDecimal previousStock = product.getCurrentStock();
            productLotService.reversePurchaseLot(purchaseItemId, quantity);

            Product refreshed = refreshProduct(product.getId());
            return recordMovementAfterStockChange(
                    refreshed,
                    InventoryMovementType.PURCHASE_CANCEL,
                    quantity,
                    previousStock,
                    refreshed.getCurrentStock(),
                    unitCost,
                    purchaseId,
                    null,
                    null,
                    notes);
        }

        return registerMovement(
                product,
                InventoryMovementType.PURCHASE_CANCEL,
                quantity,
                unitCost,
                purchaseId,
                null,
                notes);
    }

    /**
     * Registra un movimiento cuando el stock ya fue actualizado por el módulo de lotes.
     */
    @Transactional
    public InventoryMovement recordMovementAfterStockChange(
            Product product,
            InventoryMovementType type,
            BigDecimal quantity,
            BigDecimal previousStock,
            BigDecimal newStock,
            BigDecimal unitCost,
            UUID referencePurchaseId,
            UUID referenceSaleId,
            ProductLot lot,
            String notes) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        InventoryMovement movement = InventoryMovement.builder()
                .product(product)
                .movementType(type)
                .quantity(quantity)
                .previousStock(previousStock)
                .newStock(newStock)
                .unitCost(unitCost)
                .referencePurchaseId(referencePurchaseId)
                .referenceSaleId(referenceSaleId)
                .lot(lot)
                .notes(notes)
                .createdBy(currentUserId)
                .build();

        product.setUpdatedBy(currentUserId);
        productRepository.save(product);
        return inventoryMovementRepository.save(movement);
    }

    /**
     * Registra un movimiento de inventario aplicando el cambio de stock sobre el producto.
     * Solo para productos sin control de vencimiento.
     */
    @Transactional
    public InventoryMovement registerMovement(
            Product product,
            InventoryMovementType type,
            BigDecimal quantity,
            BigDecimal unitCost,
            UUID referencePurchaseId,
            UUID referenceSaleId,
            String notes) {
        if (product.isTracksExpiration()) {
            throw new ApiException(
                    "El producto controla vencimiento; use el flujo de lotes",
                    HttpStatus.BAD_REQUEST,
                    "TRACKS_EXPIRATION_USE_LOTS"
            );
        }

        BigDecimal previousStock = product.getCurrentStock();
        BigDecimal newStock = isIncrease(type)
                ? previousStock.add(quantity)
                : previousStock.subtract(quantity);

        if (newStock.signum() < 0) {
            throw new ApiException(
                    "El movimiento dejaría el stock en negativo. Stock actual: " + previousStock,
                    HttpStatus.BAD_REQUEST,
                    "INSUFFICIENT_STOCK"
            );
        }

        UUID currentUserId = SecurityUtils.getCurrentUserId();

        InventoryMovement movement = InventoryMovement.builder()
                .product(product)
                .movementType(type)
                .quantity(quantity)
                .previousStock(previousStock)
                .newStock(newStock)
                .unitCost(unitCost)
                .referencePurchaseId(referencePurchaseId)
                .referenceSaleId(referenceSaleId)
                .notes(notes)
                .createdBy(currentUserId)
                .build();

        product.setCurrentStock(newStock);
        product.setUpdatedBy(currentUserId);

        productRepository.save(product);
        return inventoryMovementRepository.save(movement);
    }

    private boolean isIncrease(InventoryMovementType type) {
        return switch (type) {
            case PURCHASE, ADJUSTMENT_IN, SALE_CANCEL -> true;
            case SALE, ADJUSTMENT_OUT, PURCHASE_CANCEL -> false;
        };
    }

    private Product refreshProduct(UUID productId) {
        return productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(this::productNotFound);
    }

    private Product findActiveProduct(UUID productId) {
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(this::productNotFound);
        if (!product.isActive()) {
            throw new ApiException(
                    "El producto está inactivo",
                    HttpStatus.BAD_REQUEST,
                    "PRODUCT_INACTIVE"
            );
        }
        return product;
    }

    private ApiException productNotFound() {
        return new ApiException(
                "Producto no encontrado",
                HttpStatus.NOT_FOUND,
                "PRODUCT_NOT_FOUND"
        );
    }

    private Map<String, Object> auditSnapshot(InventoryMovement movement) {
        return Map.of(
                "movementId", String.valueOf(movement.getId()),
                "productId", String.valueOf(movement.getProduct().getId()),
                "movementType", String.valueOf(movement.getMovementType()),
                "quantity", String.valueOf(movement.getQuantity()),
                "previousStock", String.valueOf(movement.getPreviousStock()),
                "newStock", String.valueOf(movement.getNewStock()),
                "unitCost", String.valueOf(movement.getUnitCost()),
                "notes", String.valueOf(movement.getNotes())
        );
    }
}
