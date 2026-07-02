package com.tiendadebarrio.products.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CategoryResponse {

    private final UUID id;
    private final String name;
    private final String description;
    private final boolean active;
}
