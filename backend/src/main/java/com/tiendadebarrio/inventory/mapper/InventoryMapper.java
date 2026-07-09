package com.tiendadebarrio.inventory.mapper;

import com.tiendadebarrio.inventory.dto.InventoryMovementResponse;
import com.tiendadebarrio.inventory.dto.LowStockResponse;
import com.tiendadebarrio.inventory.dto.ProductStockResponse;
import com.tiendadebarrio.inventory.entity.InventoryMovement;
import com.tiendadebarrio.products.entity.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class InventoryMapper {

    public InventoryMovementResponse toMovementResponse(InventoryMovement movement) {
        Product product = movement.getProduct();
        return InventoryMovementResponse.builder()
                .id(movement.getId())
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .barcode(product != null ? product.getBarcode() : null)
                .movementType(movement.getMovementType())
                .quantity(movement.getQuantity())
                .previousStock(movement.getPreviousStock())
                .newStock(movement.getNewStock())
                .unitCost(movement.getUnitCost())
                .notes(movement.getNotes())
                .createdBy(movement.getCreatedBy())
                .createdAt(movement.getCreatedAt())
                .build();
    }

    public ProductStockResponse toStockResponse(Product product, BigDecimal sellableStock) {
        return ProductStockResponse.builder()
                .productId(product.getId())
                .barcode(product.getBarcode())
                .name(product.getName())
                .currentStock(product.getCurrentStock())
                .sellableStock(sellableStock)
                .minStock(product.getMinStock())
                .tracksExpiration(product.isTracksExpiration())
                .unitMeasureName(product.getUnitMeasure() != null ? product.getUnitMeasure().getName() : null)
                .active(product.isActive())
                .build();
    }

    public LowStockResponse toLowStockResponse(Product product, BigDecimal sellableStock) {
        BigDecimal referenceStock = product.isTracksExpiration() ? sellableStock : product.getCurrentStock();
        BigDecimal missing = product.getMinStock().subtract(referenceStock);
        if (missing.signum() < 0) {
            missing = BigDecimal.ZERO;
        }
        return LowStockResponse.builder()
                .productId(product.getId())
                .barcode(product.getBarcode())
                .name(product.getName())
                .currentStock(product.getCurrentStock())
                .minStock(product.getMinStock())
                .missingQuantity(missing)
                .unitMeasureName(product.getUnitMeasure() != null ? product.getUnitMeasure().getName() : null)
                .build();
    }
}
