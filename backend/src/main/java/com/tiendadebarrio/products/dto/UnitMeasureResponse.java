package com.tiendadebarrio.products.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class UnitMeasureResponse {

    private final UUID id;
    private final String code;
    private final String name;
    private final boolean active;
}
