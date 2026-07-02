package com.tiendadebarrio.customers.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CustomerListResponse {

    private final UUID id;
    private final String fullName;
    private final String nit;
    private final String phone;
    private final String email;
    private final boolean active;
}
