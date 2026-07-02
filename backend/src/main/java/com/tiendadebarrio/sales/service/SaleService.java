package com.tiendadebarrio.sales.service;

import com.tiendadebarrio.audit.service.AuditService;
import com.tiendadebarrio.cash.service.CashService;
import com.tiendadebarrio.common.enums.PaymentMethod;
import com.tiendadebarrio.common.exception.ApiException;
import com.tiendadebarrio.customers.entity.Customer;
import com.tiendadebarrio.customers.repository.CustomerRepository;
import com.tiendadebarrio.inventory.service.InventoryService;
import com.tiendadebarrio.products.entity.Product;
import com.tiendadebarrio.products.repository.ProductRepository;
import com.tiendadebarrio.sales.dto.SaleCreateRequest;
import com.tiendadebarrio.sales.dto.SaleItemRequest;
import com.tiendadebarrio.sales.dto.SaleListResponse;
import com.tiendadebarrio.sales.dto.SaleResponse;
import com.tiendadebarrio.sales.entity.Sale;
import com.tiendadebarrio.sales.entity.SaleItem;
import com.tiendadebarrio.sales.entity.SaleStatus;
import com.tiendadebarrio.sales.mapper.SaleMapper;
import com.tiendadebarrio.sales.repository.SaleRepository;
import com.tiendadebarrio.security.SecurityUtils;
import com.tiendadebarrio.users.entity.AppUser;
import com.tiendadebarrio.users.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SaleService {

    private static final String AUDIT_MODULE = "SALES";
    private static final String AUDIT_ENTITY = "Sale";
    private static final int MONEY_SCALE = 2;

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final AppUserRepository appUserRepository;
    private final InventoryService inventoryService;
    private final CashService cashService;
    private final SaleMapper saleMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<SaleListResponse> list() {
        return saleRepository.findListView()
                .stream()
                .map(saleMapper::toListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SaleResponse getById(UUID id) {
        return saleMapper.toResponse(findSale(id));
    }

    @Transactional(readOnly = true)
    public List<SaleListResponse> today() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        return byRange(start, start.plusDays(1));
    }

    @Transactional(readOnly = true)
    public List<SaleListResponse> byDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        return byRange(start, start.plusDays(1));
    }

    private List<SaleListResponse> byRange(LocalDateTime start, LocalDateTime end) {
        return saleRepository.findByDateRange(start, end)
                .stream()
                .map(saleMapper::toListResponse)
                .toList();
    }

    @Transactional
    public SaleResponse create(SaleCreateRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();

        // Regla: debe existir una caja abierta para poder vender.
        UUID cashSessionId = cashService.getCurrentOpenSessionId();

        AppUser cashier = appUserRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        "El usuario autenticado no existe",
                        HttpStatus.UNAUTHORIZED,
                        "USER_NOT_FOUND"
                ));

        Customer customer = resolveCustomer(request.getCustomerId());
        PaymentMethod paymentMethod = request.getPaymentMethod() != null
                ? request.getPaymentMethod()
                : PaymentMethod.CASH;

        Sale sale = Sale.builder()
                .customer(customer)
                .cashSessionId(cashSessionId)
                .cashier(cashier)
                .saleDate(LocalDateTime.now())
                .paymentMethod(paymentMethod)
                .status(SaleStatus.COMPLETED)
                .notes(request.getNotes())
                .subtotal(BigDecimal.ZERO.setScale(MONEY_SCALE))
                .discountTotal(BigDecimal.ZERO.setScale(MONEY_SCALE))
                .taxTotal(BigDecimal.ZERO.setScale(MONEY_SCALE))
                .total(BigDecimal.ZERO.setScale(MONEY_SCALE))
                .createdBy(userId)
                .build();
        sale.setDeleted(false);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (SaleItemRequest itemRequest : request.getItems()) {
            Product product = findUsableProduct(itemRequest.getProductId());
            BigDecimal quantity = itemRequest.getQuantity();

            if (product.getCurrentStock().compareTo(quantity) < 0) {
                throw new ApiException(
                        "Stock insuficiente para el producto " + product.getName()
                                + ". Disponible: " + product.getCurrentStock(),
                        HttpStatus.BAD_REQUEST,
                        "INSUFFICIENT_STOCK"
                );
            }

            // Regla v1: siempre se usa products.sale_price, ignorando el unitPrice del frontend.
            BigDecimal unitPrice = product.getSalePrice();
            BigDecimal lineTotal = quantity.multiply(unitPrice).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

            SaleItem item = SaleItem.builder()
                    .product(product)
                    .barcode(product.getBarcode())
                    .productName(product.getName())
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .discountAmount(BigDecimal.ZERO.setScale(MONEY_SCALE))
                    .lineTotal(lineTotal)
                    .build();
            sale.addItem(item);

            subtotal = subtotal.add(lineTotal);
        }

        subtotal = subtotal.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        sale.setSubtotal(subtotal);
        sale.setTotal(subtotal);

        Sale saved = saleRepository.saveAndFlush(sale);

        // Descuento de inventario y movimientos tipo SALE.
        for (SaleItem item : saved.getItems()) {
            inventoryService.registerSaleMovement(
                    item.getProduct(),
                    item.getQuantity(),
                    saved.getId(),
                    "Venta");
        }

        // Ingreso en caja.
        cashService.registerSaleIncome(saved.getId(), saved.getTotal(), paymentMethod, userId);

        auditService.record("CREATE", AUDIT_MODULE, AUDIT_ENTITY, saved.getId(), null, auditSnapshot(saved));

        return saleMapper.toResponse(saved);
    }

    @Transactional
    public SaleResponse cancel(UUID id) {
        Sale sale = findSale(id);

        if (sale.getStatus() == SaleStatus.CANCELLED) {
            throw new ApiException(
                    "La venta ya está cancelada",
                    HttpStatus.CONFLICT,
                    "SALE_ALREADY_CANCELLED"
            );
        }
        if (sale.getStatus() != SaleStatus.COMPLETED) {
            throw new ApiException(
                    "Solo se puede cancelar una venta completada",
                    HttpStatus.CONFLICT,
                    "SALE_NOT_CANCELLABLE"
            );
        }

        UUID userId = SecurityUtils.getCurrentUserId();

        // Regla: debe existir una caja abierta para registrar la anulación.
        cashService.getCurrentOpenSessionId();

        // Devolución de stock y movimientos tipo SALE_CANCEL.
        for (SaleItem item : sale.getItems()) {
            inventoryService.registerSaleCancellationMovement(
                    item.getProduct(),
                    item.getQuantity(),
                    sale.getId(),
                    "Anulación de venta");
        }

        // Egreso en caja por anulación.
        cashService.registerSaleCancellationExpense(sale.getId(), sale.getTotal(), sale.getPaymentMethod(), userId);

        sale.setStatus(SaleStatus.CANCELLED);
        sale.setUpdatedBy(userId);

        Sale saved = saleRepository.save(sale);

        auditService.record("CANCEL", AUDIT_MODULE, AUDIT_ENTITY, saved.getId(), null, auditSnapshot(saved));

        return saleMapper.toResponse(saved);
    }

    private Sale findSale(UUID id) {
        return saleRepository.findDetailById(id)
                .orElseThrow(() -> new ApiException(
                        "Venta no encontrada",
                        HttpStatus.NOT_FOUND,
                        "SALE_NOT_FOUND"
                ));
    }

    private Customer resolveCustomer(UUID customerId) {
        if (customerId == null) {
            return null;
        }
        Customer customer = customerRepository.findByIdAndDeletedFalse(customerId)
                .orElseThrow(() -> new ApiException(
                        "El cliente indicado no existe",
                        HttpStatus.BAD_REQUEST,
                        "CUSTOMER_NOT_FOUND"
                ));
        if (!customer.isActive()) {
            throw new ApiException(
                    "El cliente está inactivo",
                    HttpStatus.BAD_REQUEST,
                    "CUSTOMER_INACTIVE"
            );
        }
        return customer;
    }

    private Product findUsableProduct(UUID productId) {
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new ApiException(
                        "El producto indicado no existe",
                        HttpStatus.BAD_REQUEST,
                        "PRODUCT_NOT_FOUND"
                ));
        if (!product.isActive()) {
            throw new ApiException(
                    "El producto " + product.getName() + " no está activo",
                    HttpStatus.BAD_REQUEST,
                    "PRODUCT_INACTIVE"
            );
        }
        return product;
    }

    private Map<String, Object> auditSnapshot(Sale sale) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", String.valueOf(sale.getId()));
        snapshot.put("saleNumber", String.valueOf(sale.getSaleNumber()));
        snapshot.put("status", String.valueOf(sale.getStatus()));
        snapshot.put("paymentMethod", String.valueOf(sale.getPaymentMethod()));
        snapshot.put("total", String.valueOf(sale.getTotal()));
        snapshot.put("itemsCount", String.valueOf(sale.getItems().size()));
        snapshot.put("customerId", sale.getCustomer() != null ? String.valueOf(sale.getCustomer().getId()) : "CONSUMIDOR_FINAL");
        return snapshot;
    }
}
