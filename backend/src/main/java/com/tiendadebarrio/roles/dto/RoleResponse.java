package com.tiendadebarrio.roles.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class RoleResponse {

    private final UUID id;
    private final String name;
    private final String description;
    private final boolean active;
}
