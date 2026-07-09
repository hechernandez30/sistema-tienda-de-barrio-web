package com.tiendadebarrio.products.mapper;

import com.tiendadebarrio.inventory.service.ProductLotService;
import com.tiendadebarrio.products.dto.ProductDetailResponse;
import com.tiendadebarrio.products.dto.ProductListResponse;
import com.tiendadebarrio.products.dto.ProductPosResponse;
import com.tiendadebarrio.products.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final ProductLotService productLotService;

    public ProductListResponse toListResponse(Product product) {
        return ProductListResponse.builder()
                .id(product.getId())
                .barcode(product.getBarcode())
                .sku(product.getSku())
                .name(product.getName())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .unitMeasureName(product.getUnitMeasure() != null ? product.getUnitMeasure().getName() : null)
                .salePrice(product.getSalePrice())
                .currentStock(product.getCurrentStock())
                .minStock(product.getMinStock())
                .active(product.isActive())
                .build();
    }

    public ProductDetailResponse toDetailResponse(Product product) {
        BigDecimal sellable = productLotService.getSellableQuantity(product);
        return ProductDetailResponse.builder()
                .id(product.getId())
                .barcode(product.getBarcode())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .unitMeasureId(product.getUnitMeasure() != null ? product.getUnitMeasure().getId() : null)
                .unitMeasureName(product.getUnitMeasure() != null ? product.getUnitMeasure().getName() : null)
                .purchasePrice(product.getPurchasePrice())
                .salePrice(product.getSalePrice())
                .minStock(product.getMinStock())
                .currentStock(product.getCurrentStock())
                .sellableStock(sellable)
                .tracksExpiration(product.isTracksExpiration())
                .active(product.isActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public ProductPosResponse toPosResponse(Product product) {
        BigDecimal sellable = productLotService.getSellableQuantity(product);
        return ProductPosResponse.builder()
                .id(product.getId())
                .barcode(product.getBarcode())
                .name(product.getName())
                .salePrice(product.getSalePrice())
                .currentStock(product.getCurrentStock())
                .sellableStock(sellable)
                .tracksExpiration(product.isTracksExpiration())
                .unitMeasureName(product.getUnitMeasure() != null ? product.getUnitMeasure().getName() : null)
                .build();
    }
}
