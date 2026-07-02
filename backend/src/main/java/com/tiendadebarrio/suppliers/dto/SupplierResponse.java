package com.tiendadebarrio.suppliers.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class SupplierResponse {

    private final UUID id;
    private final String name;
    private final String nit;
    private final String contactName;
    private final String phone;
    private final String email;
    private final String address;
    private final boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
