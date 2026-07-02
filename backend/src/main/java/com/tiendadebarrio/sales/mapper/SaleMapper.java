package com.tiendadebarrio.sales.mapper;

import com.tiendadebarrio.customers.entity.Customer;
import com.tiendadebarrio.sales.dto.SaleItemResponse;
import com.tiendadebarrio.sales.dto.SaleListResponse;
import com.tiendadebarrio.sales.dto.SaleResponse;
import com.tiendadebarrio.sales.entity.Sale;
import com.tiendadebarrio.sales.entity.SaleItem;
import com.tiendadebarrio.users.entity.AppUser;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SaleMapper {

    public SaleResponse toResponse(Sale sale) {
        List<SaleItemResponse> items = sale.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return SaleResponse.builder()
                .id(sale.getId())
                .saleNumber(sale.getSaleNumber())
                .customer(toCustomerSummary(sale.getCustomer()))
                .cashier(toCashierSummary(sale.getCashier()))
                .cashSessionId(sale.getCashSessionId())
                .saleDate(sale.getSaleDate())
                .paymentMethod(sale.getPaymentMethod())
                .subtotal(sale.getSubtotal())
                .discountTotal(sale.getDiscountTotal())
                .taxTotal(sale.getTaxTotal())
                .total(sale.getTotal())
                .status(sale.getStatus())
                .notes(sale.getNotes())
                .items(items)
                .createdAt(sale.getCreatedAt())
                .build();
    }

    public SaleListResponse toListResponse(Sale sale) {
        return SaleListResponse.builder()
                .id(sale.getId())
                .saleNumber(sale.getSaleNumber())
                .customerName(sale.getCustomer() != null ? sale.getCustomer().getFullName() : null)
                .cashierName(fullName(sale.getCashier()))
                .saleDate(sale.getSaleDate())
                .paymentMethod(sale.getPaymentMethod())
                .total(sale.getTotal())
                .status(sale.getStatus())
                .build();
    }

    private SaleItemResponse toItemResponse(SaleItem item) {
        return SaleItemResponse.builder()
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .barcode(item.getBarcode())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .discountAmount(item.getDiscountAmount())
                .lineTotal(item.getLineTotal())
                .build();
    }

    private SaleResponse.CustomerSummary toCustomerSummary(Customer customer) {
        if (customer == null) {
            return null;
        }
        return SaleResponse.CustomerSummary.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .nit(customer.getNit())
                .build();
    }

    private SaleResponse.CashierSummary toCashierSummary(AppUser cashier) {
        if (cashier == null) {
            return null;
        }
        return SaleResponse.CashierSummary.builder()
                .id(cashier.getId())
                .username(cashier.getUsername())
                .fullName(fullName(cashier))
                .build();
    }

    private String fullName(AppUser user) {
        if (user == null) {
            return null;
        }
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }
}
