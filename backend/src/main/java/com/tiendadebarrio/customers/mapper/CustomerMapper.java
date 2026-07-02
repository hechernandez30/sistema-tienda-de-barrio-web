package com.tiendadebarrio.customers.mapper;

import com.tiendadebarrio.customers.dto.CustomerListResponse;
import com.tiendadebarrio.customers.dto.CustomerResponse;
import com.tiendadebarrio.customers.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public CustomerResponse toResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .nit(customer.getNit())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .address(customer.getAddress())
                .active(customer.isActive())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    public CustomerListResponse toListResponse(Customer customer) {
        return CustomerListResponse.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .nit(customer.getNit())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .active(customer.isActive())
                .build();
    }
}
