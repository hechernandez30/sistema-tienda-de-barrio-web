package com.tiendadebarrio.customers.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CustomerResponse {

    private final UUID id;
    private final String fullName;
    private final String nit;
    private final String phone;
    private final String email;
    private final String address;
    private final boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
