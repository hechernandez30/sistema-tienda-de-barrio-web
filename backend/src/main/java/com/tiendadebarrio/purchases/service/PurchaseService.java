package com.tiendadebarrio.purchases.service;

import com.tiendadebarrio.audit.service.AuditService;
import com.tiendadebarrio.common.enums.PaymentMethod;
import com.tiendadebarrio.common.exception.ApiException;
import com.tiendadebarrio.inventory.entity.InventoryMovementType;
import com.tiendadebarrio.inventory.service.InventoryService;
import com.tiendadebarrio.products.entity.Product;
import com.tiendadebarrio.products.repository.ProductRepository;
import com.tiendadebarrio.purchases.dto.PurchaseCreateRequest;
import com.tiendadebarrio.purchases.dto.PurchaseItemRequest;
import com.tiendadebarrio.purchases.dto.PurchaseListResponse;
import com.tiendadebarrio.purchases.dto.PurchaseResponse;
import com.tiendadebarrio.purchases.entity.Purchase;
import com.tiendadebarrio.purchases.entity.PurchaseItem;
import com.tiendadebarrio.purchases.entity.PurchaseStatus;
import com.tiendadebarrio.purchases.mapper.PurchaseMapper;
import com.tiendadebarrio.purchases.repository.PurchaseRepository;
import com.tiendadebarrio.security.SecurityUtils;
import com.tiendadebarrio.suppliers.entity.Supplier;
import com.tiendadebarrio.suppliers.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseService {

    private static final String AUDIT_MODULE = "PURCHASES";
    private static final String AUDIT_ENTITY = "Purchase";
    private static final int MONEY_SCALE = 2;

    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final PurchaseMapper purchaseMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<PurchaseListResponse> list() {
        return purchaseRepository.findListView()
                .stream()
                .map(purchaseMapper::toListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PurchaseResponse getById(UUID id) {
        return purchaseMapper.toResponse(findPurchase(id));
    }

    @Transactional
    public PurchaseResponse create(PurchaseCreateRequest request) {
        boolean paid = Boolean.TRUE.equals(request.getIsPaid());
        validatePayment(paid, request.getPaymentMethod());

        Supplier supplier = supplierRepository.findByIdAndDeletedFalse(request.getSupplierId())
                .orElseThrow(() -> new ApiException(
                        "El proveedor indicado no existe",
                        HttpStatus.BAD_REQUEST,
                        "SUPPLIER_NOT_FOUND"
                ));

        UUID currentUserId = SecurityUtils.getCurrentUserId();

        Purchase purchase = Purchase.builder()
                .supplier(supplier)
                .createdByUserId(currentUserId)
                .purchaseDate(request.getPurchaseDate() != null ? request.getPurchaseDate() : LocalDateTime.now())
                .status(PurchaseStatus.DRAFT)
                .paid(paid)
                .paymentMethod(paid ? request.getPaymentMethod() : null)
                .notes(request.getNotes())
                .discountTotal(BigDecimal.ZERO.setScale(MONEY_SCALE))
                .taxTotal(BigDecimal.ZERO.setScale(MONEY_SCALE))
                .subtotal(BigDecimal.ZERO.setScale(MONEY_SCALE))
                .total(BigDecimal.ZERO.setScale(MONEY_SCALE))
                .createdBy(currentUserId)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        for (PurchaseItemRequest itemRequest : request.getItems()) {
            Product product = findUsableProduct(itemRequest.getProductId());
            BigDecimal lineTotal = itemRequest.getQuantity()
                    .multiply(itemRequest.getUnitCost())
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

            PurchaseItem item = PurchaseItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitCost(itemRequest.getUnitCost())
                    .lineTotal(lineTotal)
                    .build();
            purchase.addItem(item);

            subtotal = subtotal.add(lineTotal);
        }

        subtotal = subtotal.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        purchase.setSubtotal(subtotal);
        purchase.setTotal(subtotal.subtract(purchase.getDiscountTotal()).add(purchase.getTaxTotal()));

        Purchase saved = purchaseRepository.save(purchase);

        auditService.record("CREATE", AUDIT_MODULE, AUDIT_ENTITY, saved.getId(), null, auditSnapshot(saved));

        return purchaseMapper.toResponse(saved);
    }

    @Transactional
    public PurchaseResponse confirm(UUID id) {
        Purchase purchase = findPurchase(id);

        if (purchase.getStatus() == PurchaseStatus.CONFIRMED) {
            throw new ApiException(
                    "La compra ya está confirmada",
                    HttpStatus.CONFLICT,
                    "PURCHASE_ALREADY_CONFIRMED"
            );
        }
        if (purchase.getStatus() == PurchaseStatus.CANCELLED) {
            throw new ApiException(
                    "No se puede confirmar una compra cancelada",
                    HttpStatus.CONFLICT,
                    "PURCHASE_CANCELLED"
            );
        }

        for (PurchaseItem item : purchase.getItems()) {
            Product product = ensureUsable(item.getProduct());
            inventoryService.registerMovement(
                    product,
                    InventoryMovementType.PURCHASE,
                    item.getQuantity(),
                    item.getUnitCost(),
                    purchase.getId(),
                    null,
                    "Confirmación de compra");
        }

        purchase.setStatus(PurchaseStatus.CONFIRMED);
        purchase.setUpdatedBy(SecurityUtils.getCurrentUserId());

        Purchase saved = purchaseRepository.save(purchase);

        applyCashEgressIfPaid(saved);

        auditService.record("CONFIRM", AUDIT_MODULE, AUDIT_ENTITY, saved.getId(), null, auditSnapshot(saved));

        return purchaseMapper.toResponse(saved);
    }

    @Transactional
    public PurchaseResponse cancel(UUID id) {
        Purchase purchase = findPurchase(id);

        if (purchase.getStatus() == PurchaseStatus.CANCELLED) {
            throw new ApiException(
                    "La compra ya está cancelada",
                    HttpStatus.CONFLICT,
                    "PURCHASE_ALREADY_CANCELLED"
            );
        }

        if (purchase.getStatus() == PurchaseStatus.CONFIRMED) {
            for (PurchaseItem item : purchase.getItems()) {
                Product product = ensureUsable(item.getProduct());
                inventoryService.registerMovement(
                        product,
                        InventoryMovementType.PURCHASE_CANCEL,
                        item.getQuantity(),
                        item.getUnitCost(),
                        purchase.getId(),
                        null,
                        "Cancelación de compra");
            }
        }

        purchase.setStatus(PurchaseStatus.CANCELLED);
        purchase.setUpdatedBy(SecurityUtils.getCurrentUserId());

        Purchase saved = purchaseRepository.save(purchase);

        auditService.record("CANCEL", AUDIT_MODULE, AUDIT_ENTITY, saved.getId(), null, auditSnapshot(saved));

        return purchaseMapper.toResponse(saved);
    }

    /**
     * Punto de extensión para el futuro módulo de Caja. Cuando una compra pagada se
     * confirme, aquí deberá registrarse un egreso de caja. Por ahora no hace nada porque
     * el módulo de caja aún no existe.
     */
    private void applyCashEgressIfPaid(Purchase purchase) {
        if (purchase.isPaid()) {
            log.debug("Compra {} pagada confirmada. Egreso de caja pendiente hasta implementar el módulo de caja.",
                    purchase.getId());
            // TODO: registrar egreso en caja cuando el módulo de caja esté disponible.
        }
    }

    private void validatePayment(boolean paid, PaymentMethod paymentMethod) {
        if (paid && paymentMethod == null) {
            throw new ApiException(
                    "El método de pago es obligatorio cuando la compra está pagada",
                    HttpStatus.BAD_REQUEST,
                    "PAYMENT_METHOD_REQUIRED"
            );
        }
    }

    private Purchase findPurchase(UUID id) {
        return purchaseRepository.findDetailById(id)
                .orElseThrow(() -> new ApiException(
                        "Compra no encontrada",
                        HttpStatus.NOT_FOUND,
                        "PURCHASE_NOT_FOUND"
                ));
    }

    private Product findUsableProduct(UUID productId) {
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new ApiException(
                        "El producto indicado no existe",
                        HttpStatus.BAD_REQUEST,
                        "PRODUCT_NOT_FOUND"
                ));
        return ensureUsable(product);
    }

    private Product ensureUsable(Product product) {
        if (product.isDeleted() || !product.isActive()) {
            throw new ApiException(
                    "El producto " + product.getName() + " no está disponible (inactivo o eliminado)",
                    HttpStatus.BAD_REQUEST,
                    "PRODUCT_NOT_AVAILABLE"
            );
        }
        return product;
    }

    private Map<String, Object> auditSnapshot(Purchase purchase) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", String.valueOf(purchase.getId()));
        snapshot.put("purchaseNumber", String.valueOf(purchase.getPurchaseNumber()));
        snapshot.put("status", String.valueOf(purchase.getStatus()));
        snapshot.put("paid", String.valueOf(purchase.isPaid()));
        snapshot.put("paymentMethod", String.valueOf(purchase.getPaymentMethod()));
        snapshot.put("total", String.valueOf(purchase.getTotal()));
        snapshot.put("itemsCount", String.valueOf(purchase.getItems().size()));
        return snapshot;
    }
}
