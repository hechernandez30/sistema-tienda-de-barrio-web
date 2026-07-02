package com.tiendadebarrio.users.dto;

import com.tiendadebarrio.roles.dto.RoleResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class UserResponse {

    private final UUID id;
    private final RoleResponse role;
    private final String username;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final boolean active;
    private final LocalDateTime lastLoginAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
