package com.tiendadebarrio.products.service;

import com.tiendadebarrio.audit.service.AuditService;
import com.tiendadebarrio.common.exception.ApiException;
import com.tiendadebarrio.inventory.service.InventoryService;
import com.tiendadebarrio.products.dto.ProductCreateRequest;
import com.tiendadebarrio.products.dto.ProductDetailResponse;
import com.tiendadebarrio.products.dto.ProductListResponse;
import com.tiendadebarrio.products.dto.ProductPosResponse;
import com.tiendadebarrio.products.dto.ProductUpdateRequest;
import com.tiendadebarrio.products.entity.Product;
import com.tiendadebarrio.products.entity.ProductCategory;
import com.tiendadebarrio.products.entity.UnitMeasure;
import com.tiendadebarrio.products.mapper.ProductMapper;
import com.tiendadebarrio.products.repository.ProductCategoryRepository;
import com.tiendadebarrio.products.repository.ProductRepository;
import com.tiendadebarrio.products.repository.UnitMeasureRepository;
import com.tiendadebarrio.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final String AUDIT_MODULE = "PRODUCTS";
    private static final String AUDIT_ENTITY = "Product";
    private static final int SEARCH_LIMIT = 20;

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final UnitMeasureRepository unitMeasureRepository;
    private final ProductMapper productMapper;
    private final AuditService auditService;
    private final InventoryService inventoryService;

    @Transactional(readOnly = true)
    public List<ProductListResponse> list() {
        return productRepository.findByDeletedFalseOrderByNameAsc()
                .stream()
                .map(productMapper::toListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getById(UUID id) {
        return productMapper.toDetailResponse(findActiveProduct(id));
    }

    @Transactional(readOnly = true)
    public ProductPosResponse getByBarcode(String barcode) {
        Product product = productRepository.findByBarcodeAndDeletedFalseAndActiveTrue(barcode)
                .orElseThrow(() -> new ApiException(
                        "No se encontró un producto activo con el código de barras indicado",
                        HttpStatus.NOT_FOUND,
                        "PRODUCT_NOT_FOUND"
                ));
        return productMapper.toPosResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductPosResponse> search(String term) {
        if (!StringUtils.hasText(term)) {
            return List.of();
        }
        return productRepository
                .findByDeletedFalseAndActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(
                        term.trim(),
                        PageRequest.of(0, SEARCH_LIMIT))
                .stream()
                .map(productMapper::toPosResponse)
                .toList();
    }

    @Transactional
    public ProductDetailResponse create(ProductCreateRequest request) {
        String barcode = request.getBarcode().trim();
        if (productRepository.existsByBarcodeAndDeletedFalse(barcode)) {
            throw new ApiException(
                    "Ya existe un producto con el código de barras " + barcode,
                    HttpStatus.CONFLICT,
                    "PRODUCT_BARCODE_DUPLICATED"
            );
        }

        String sku = normalizeSku(request.getSku());
        if (sku != null && productRepository.existsBySkuAndDeletedFalse(sku)) {
            throw new ApiException(
                    "Ya existe un producto con el SKU " + sku,
                    HttpStatus.CONFLICT,
                    "PRODUCT_SKU_DUPLICATED"
            );
        }

        UUID currentUserId = SecurityUtils.getCurrentUserId();
        BigDecimal initialStock = defaultAmount(request.getCurrentStock());

        Product product = Product.builder()
                .barcode(barcode)
                .sku(sku)
                .name(request.getName().trim())
                .description(request.getDescription())
                .category(resolveCategory(request.getCategoryId()))
                .unitMeasure(resolveUnitMeasure(request.getUnitMeasureId()))
                .purchasePrice(defaultAmount(request.getPurchasePrice()))
                .salePrice(defaultAmount(request.getSalePrice()))
                .minStock(defaultAmount(request.getMinStock()))
                .currentStock(BigDecimal.ZERO)
                .active(request.getActive() == null || request.getActive())
                .createdBy(currentUserId)
                .build();
        product.setDeleted(false);

        Product saved = productRepository.save(product);

        auditService.record("CREATE", AUDIT_MODULE, AUDIT_ENTITY, saved.getId(), null, auditSnapshot(saved));

        if (initialStock.signum() > 0) {
            inventoryService.registerInitialStock(saved, initialStock, saved.getPurchasePrice());
        }

        return productMapper.toDetailResponse(saved);
    }

    @Transactional
    public ProductDetailResponse update(UUID id, ProductUpdateRequest request) {
        Product product = findActiveProduct(id);
        Map<String, Object> previousSnapshot = auditSnapshot(product);

        String barcode = request.getBarcode().trim();
        if (productRepository.existsByBarcodeAndDeletedFalseAndIdNot(barcode, id)) {
            throw new ApiException(
                    "Ya existe un producto con el código de barras " + barcode,
                    HttpStatus.CONFLICT,
                    "PRODUCT_BARCODE_DUPLICATED"
            );
        }

        String sku = normalizeSku(request.getSku());
        if (sku != null && productRepository.existsBySkuAndDeletedFalseAndIdNot(sku, id)) {
            throw new ApiException(
                    "Ya existe un producto con el SKU " + sku,
                    HttpStatus.CONFLICT,
                    "PRODUCT_SKU_DUPLICATED"
            );
        }

        product.setBarcode(barcode);
        product.setSku(sku);
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setCategory(resolveCategory(request.getCategoryId()));
        product.setUnitMeasure(resolveUnitMeasure(request.getUnitMeasureId()));
        product.setPurchasePrice(defaultAmount(request.getPurchasePrice()));
        product.setSalePrice(defaultAmount(request.getSalePrice()));
        product.setMinStock(defaultAmount(request.getMinStock()));
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
        product.setUpdatedBy(SecurityUtils.getCurrentUserId());

        Product saved = productRepository.save(product);

        auditService.record("UPDATE", AUDIT_MODULE, AUDIT_ENTITY, saved.getId(), previousSnapshot, auditSnapshot(saved));

        return productMapper.toDetailResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Product product = findActiveProduct(id);
        Map<String, Object> previousSnapshot = auditSnapshot(product);

        product.setDeleted(true);
        product.setDeletedAt(LocalDateTime.now());
        product.setDeletedBy(SecurityUtils.getCurrentUserId());
        product.setActive(false);

        productRepository.save(product);

        auditService.record("DELETE", AUDIT_MODULE, AUDIT_ENTITY, product.getId(), previousSnapshot, null);
    }

    private Product findActiveProduct(UUID id) {
        return productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ApiException(
                        "Producto no encontrado",
                        HttpStatus.NOT_FOUND,
                        "PRODUCT_NOT_FOUND"
                ));
    }

    private ProductCategory resolveCategory(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }
        return productCategoryRepository.findByIdAndDeletedFalse(categoryId)
                .orElseThrow(() -> new ApiException(
                        "La categoría indicada no existe",
                        HttpStatus.BAD_REQUEST,
                        "CATEGORY_NOT_FOUND"
                ));
    }

    private UnitMeasure resolveUnitMeasure(UUID unitMeasureId) {
        if (unitMeasureId == null) {
            return null;
        }
        return unitMeasureRepository.findByIdAndDeletedFalse(unitMeasureId)
                .orElseThrow(() -> new ApiException(
                        "La unidad de medida indicada no existe",
                        HttpStatus.BAD_REQUEST,
                        "UNIT_MEASURE_NOT_FOUND"
                ));
    }

    private String normalizeSku(String sku) {
        return StringUtils.hasText(sku) ? sku.trim() : null;
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Map<String, Object> auditSnapshot(Product product) {
        return Map.of(
                "id", String.valueOf(product.getId()),
                "barcode", String.valueOf(product.getBarcode()),
                "sku", String.valueOf(product.getSku()),
                "name", String.valueOf(product.getName()),
                "purchasePrice", String.valueOf(product.getPurchasePrice()),
                "salePrice", String.valueOf(product.getSalePrice()),
                "minStock", String.valueOf(product.getMinStock()),
                "currentStock", String.valueOf(product.getCurrentStock()),
                "active", String.valueOf(product.isActive())
        );
    }
}
