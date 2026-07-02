package com.tiendadebarrio.inventory.service;

import com.tiendadebarrio.audit.service.AuditService;
import com.tiendadebarrio.common.exception.ApiException;
import com.tiendadebarrio.inventory.dto.InventoryAdjustmentRequest;
import com.tiendadebarrio.inventory.dto.InventoryMovementResponse;
import com.tiendadebarrio.inventory.dto.LowStockResponse;
import com.tiendadebarrio.inventory.dto.ProductStockResponse;
import com.tiendadebarrio.inventory.entity.InventoryMovement;
import com.tiendadebarrio.inventory.entity.InventoryMovementType;
import com.tiendadebarrio.inventory.mapper.InventoryMapper;
import com.tiendadebarrio.inventory.repository.InventoryMovementRepository;
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
        return inventoryMapper.toStockResponse(product);
    }

    @Transactional(readOnly = true)
    public List<LowStockResponse> lowStock() {
        return productRepository.findLowStock()
                .stream()
                .map(inventoryMapper::toLowStockResponse)
                .toList();
    }

    @Transactional
    public InventoryMovementResponse adjustIn(InventoryAdjustmentRequest request) {
        return registerAdjustment(request, InventoryMovementType.ADJUSTMENT_IN);
    }

    @Transactional
    public InventoryMovementResponse adjustOut(InventoryAdjustmentRequest request) {
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

    /**
     * Registra el stock inicial de un producto recién creado como un movimiento tipo
     * ADJUSTMENT_IN con nota "Inventario inicial". Uso interno desde el módulo de productos,
     * sin depender de un request HTTP. Debe invocarse únicamente al crear el producto y
     * solo cuando la cantidad inicial es mayor que cero.
     */
    @Transactional
    public InventoryMovement registerInitialStock(Product product, BigDecimal quantity, BigDecimal unitCost) {
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

    /**
     * Descuenta stock por una venta (movimiento tipo SALE). Reutilizable por el módulo de ventas.
     */
    @Transactional
    public InventoryMovement registerSaleMovement(Product product, BigDecimal quantity, UUID saleId, String notes) {
        return registerMovement(product, InventoryMovementType.SALE, quantity, null, null, saleId, notes);
    }

    /**
     * Devuelve stock por la anulación de una venta (movimiento tipo SALE_CANCEL).
     */
    @Transactional
    public InventoryMovement registerSaleCancellationMovement(
            Product product,
            BigDecimal quantity,
            UUID saleId,
            String notes) {
        return registerMovement(product, InventoryMovementType.SALE_CANCEL, quantity, null, null, saleId, notes);
    }

    /**
     * Registra un movimiento de inventario aplicando el cambio de stock sobre el producto.
     * Reutilizable por otros módulos (por ejemplo compras y ventas) para movimientos tipo
     * PURCHASE / PURCHASE_CANCEL / SALE / SALE_CANCEL. No registra bitácora: la trazabilidad
     * de negocio la gestiona el módulo llamante (compra o venta registra su propia bitácora).
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
