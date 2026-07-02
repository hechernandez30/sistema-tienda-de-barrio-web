package com.tiendadebarrio.suppliers.mapper;

import com.tiendadebarrio.suppliers.dto.SupplierListResponse;
import com.tiendadebarrio.suppliers.dto.SupplierResponse;
import com.tiendadebarrio.suppliers.entity.Supplier;
import org.springframework.stereotype.Component;

@Component
public class SupplierMapper {

    public SupplierResponse toResponse(Supplier supplier) {
        return SupplierResponse.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .nit(supplier.getNit())
                .contactName(supplier.getContactName())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .active(supplier.isActive())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }

    public SupplierListResponse toListResponse(Supplier supplier) {
        return SupplierListResponse.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .nit(supplier.getNit())
                .contactName(supplier.getContactName())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .active(supplier.isActive())
                .build();
    }
}
