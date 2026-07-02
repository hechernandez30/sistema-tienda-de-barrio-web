package com.tiendadebarrio.suppliers.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class SupplierListResponse {

    private final UUID id;
    private final String name;
    private final String nit;
    private final String contactName;
    private final String phone;
    private final String email;
    private final boolean active;
}
