package com.tiendadebarrio.purchases.mapper;

import com.tiendadebarrio.products.entity.Product;
import com.tiendadebarrio.purchases.dto.PurchaseItemResponse;
import com.tiendadebarrio.purchases.dto.PurchaseListResponse;
import com.tiendadebarrio.purchases.dto.PurchaseResponse;
import com.tiendadebarrio.purchases.entity.Purchase;
import com.tiendadebarrio.purchases.entity.PurchaseItem;
import com.tiendadebarrio.suppliers.entity.Supplier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PurchaseMapper {

    public PurchaseResponse toResponse(Purchase purchase) {
        List<PurchaseItemResponse> items = purchase.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return PurchaseResponse.builder()
                .id(purchase.getId())
                .purchaseNumber(purchase.getPurchaseNumber())
                .supplier(toSupplierSummary(purchase.getSupplier()))
                .purchaseDate(purchase.getPurchaseDate())
                .status(purchase.getStatus())
                .paid(purchase.isPaid())
                .paymentMethod(purchase.getPaymentMethod())
                .subtotal(purchase.getSubtotal())
                .discountTotal(purchase.getDiscountTotal())
                .taxTotal(purchase.getTaxTotal())
                .total(purchase.getTotal())
                .notes(purchase.getNotes())
                .items(items)
                .createdAt(purchase.getCreatedAt())
                .build();
    }

    public PurchaseListResponse toListResponse(Purchase purchase) {
        return PurchaseListResponse.builder()
                .id(purchase.getId())
                .purchaseNumber(purchase.getPurchaseNumber())
                .supplierName(purchase.getSupplier() != null ? purchase.getSupplier().getName() : null)
                .purchaseDate(purchase.getPurchaseDate())
                .status(purchase.getStatus())
                .paid(purchase.isPaid())
                .total(purchase.getTotal())
                .createdAt(purchase.getCreatedAt())
                .build();
    }

    private PurchaseItemResponse toItemResponse(PurchaseItem item) {
        Product product = item.getProduct();
        return PurchaseItemResponse.builder()
                .id(item.getId())
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .barcode(product != null ? product.getBarcode() : null)
                .quantity(item.getQuantity())
                .unitCost(item.getUnitCost())
                .lineTotal(item.getLineTotal())
                .build();
    }

    private PurchaseResponse.SupplierSummary toSupplierSummary(Supplier supplier) {
        if (supplier == null) {
            return null;
        }
        return PurchaseResponse.SupplierSummary.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .nit(supplier.getNit())
                .build();
    }
}
